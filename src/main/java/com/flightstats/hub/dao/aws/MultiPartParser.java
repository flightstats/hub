package com.flightstats.hub.dao.aws;

import com.flightstats.hub.app.HubProperties;
import com.flightstats.hub.exception.ContentTooLargeException;
import com.flightstats.hub.model.BatchContent;
import com.flightstats.hub.model.Content;
import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.util.ByteRing;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;

public class MultiPartParser {
    private final static Logger logger = LoggerFactory.getLogger(MultiPartParser.class);

    private static final int maxBytes = HubProperties.getProperty("app.maxPayloadSizeMB", 20) * 1024 * 1024;
    private BatchContent content;
    private BufferedInputStream stream;
    private final DecimalFormat format = new DecimalFormat("000000");
    private final ContentKey masterKey;
    private Content.Builder builder;
    private final ByteArrayOutputStream baos;

    public MultiPartParser(BatchContent content) {
        this.content = content;
        masterKey = new ContentKey();
        builder = Content.builder();
        stream = new BufferedInputStream(content.getStream());
        baos = new ByteArrayOutputStream();
    }

    public void parse() throws IOException {
        String boundary = "--" + StringUtils.substringAfter(content.getContentType(), "boundary=");
        byte[] startBoundary = (boundary + "\r\n").getBytes();
        byte[] endBoundary = (boundary + "--").getBytes();
        byte[] crlf = "\r\n".getBytes();
        boolean started = false;
        boolean header = false;
        ByteRing byteRing = new ByteRing(endBoundary.length);
        int read = stream.read();
        int count = 0;

        while (read != -1) {
            count++;
            if (count > maxBytes) {
                throw new ContentTooLargeException("max payload size is " + maxBytes + " bytes");
            }
            baos.write((byte) read);
            byteRing.put((byte) read);

            if (byteRing.compare(startBoundary)) {
                if (!started) {
                    started = true;
                    baos.reset();
                } else {
                    addItem(startBoundary);
                }
                header = true;
                builder.withContentType("text/plain");
            } else if (header && byteRing.compare(crlf)) {
                if (baos.toString().startsWith("Content")) {
                    String headerLine = StringUtils.strip(baos.toString());
                    logger.trace("header line '{}'", headerLine);
                    if (StringUtils.startsWithIgnoreCase(headerLine, "content-type:")) {
                        String type = StringUtils.trim(StringUtils.removeStartIgnoreCase(headerLine, "content-type:"));
                        builder.withContentType(type);
                    }
                } else {
                    header = false;
                }
                baos.reset();
            } else if (byteRing.compare(endBoundary)) {
                addItem(endBoundary);
                return;
            }
            read = stream.read();
        }
    }

    private void addItem(byte[] boundary) {
        String data = StringUtils.strip(StringUtils.removeEnd(baos.toString(), new String(boundary)));
        builder.withData(data.getBytes());
        ContentKey contentKey = new ContentKey(masterKey.getTime(), masterKey.getHash() +
                format.format(content.getItems().size()));
        builder.withContentKey(contentKey);
        content.getItems().add(builder.build());
        builder = Content.builder();
        baos.reset();
    }

    private String getData(byte[] boundary, ByteArrayOutputStream baos) {
        return StringUtils.strip(StringUtils.removeEnd(baos.toString(), new String(boundary)));
    }


}
