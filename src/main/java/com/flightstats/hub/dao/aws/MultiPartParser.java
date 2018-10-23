package com.flightstats.hub.dao.aws;

import com.flightstats.hub.app.HubProperties;
import com.flightstats.hub.exception.ContentTooLargeException;
import com.flightstats.hub.exception.InvalidRequestException;
import com.flightstats.hub.model.BulkContent;
import com.flightstats.hub.model.Content;
import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.util.ByteRing;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class MultiPartParser {

    private final static Logger logger = LoggerFactory.getLogger(MultiPartParser.class);
    private final static byte[] CRLF = "\r\n".getBytes();

    private final BulkContent bulkContent;
    private final BufferedInputStream stream;
    private final ByteArrayOutputStream baos;
    private final int maxBytes;

    private Content.Builder builder;

    @Inject
    public MultiPartParser(BulkContent bulkContent, HubProperties hubProperties) {
        this.bulkContent = bulkContent;
        this.stream = new BufferedInputStream(bulkContent.getStream());
        this.baos = new ByteArrayOutputStream();
        this.maxBytes = hubProperties.getProperty("app.maxPayloadSizeMB", 40) * 1024 * 1024 * 3;
        this.builder = Content.builder();
    }

    public void parse() throws IOException {
        parseItems();
        if (bulkContent.getItems().isEmpty()) {
            throw new InvalidRequestException("multipart has no items");
        } else if (bulkContent.isNew()) {
            ContentKey masterKey = new ContentKey();
            bulkContent.setMasterKey(masterKey);
            for (int i = 0; i < bulkContent.getItems().size(); i++) {
                bulkContent.getItems().get(i).setContentKey(ContentKey.bulkKey(masterKey, i));
            }
        }
    }

    private void parseItems() throws IOException {
        String boundary = "--" + getBoundary();
        byte[] startBoundary = (boundary + "\r\n").getBytes();
        byte[] endBoundary = (boundary + "--").getBytes();
        boolean started = false;
        boolean header = false;
        ByteRing byteRing = new ByteRing(endBoundary.length);
        int count = 0;
        int read = stream.read();
        while (read != -1) {
            count++;
            if (count > maxBytes) {
                logger.warn("multipart max payload exceeded {}", maxBytes, bulkContent.getChannel());
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
            } else if (header && byteRing.compare(CRLF)) {
                String headerLine = StringUtils.strip(baos.toString());
                baos.reset();
                if (StringUtils.isEmpty(headerLine)) {
                    header = false;
                } else {
                    if (StringUtils.startsWithIgnoreCase(headerLine, "content-type:")) {
                        String type = StringUtils.trim(StringUtils.removeStartIgnoreCase(headerLine, "content-type:"));
                        builder.withContentType(type);
                    } else if (StringUtils.startsWithIgnoreCase(headerLine, "content-key:")) {
                        String key = StringUtils.trim(StringUtils.removeStartIgnoreCase(headerLine, "content-key:"));
                        builder.withContentKey(ContentKey.fromFullUrl(key));
                    }
                }
            } else if (byteRing.compare(endBoundary)) {
                addItem(endBoundary);
                break;
            }
            read = stream.read();
        }
    }

    private String getBoundary() {
        //todo - gfm - 11/4/15 - this should handle an ending ';' in the content type
        return StringUtils.removeEnd(
                StringUtils.removeStart(
                        StringUtils.trim(
                                StringUtils.substringAfter(bulkContent.getContentType(), "boundary=")), "\""), "\"");

    }

    private void addItem(byte[] boundary) {
        byte[] bytes = baos.toByteArray();
        byte[] data = ArrayUtils.subarray(bytes, 0, bytes.length - boundary.length - CRLF.length);
        if ((data.length == 0 && builder.getContentKey().isPresent())
                || data.length > 0) {
            builder.withData(data);
            bulkContent.getItems().add(builder.build());
        }
        builder = Content.builder();
        baos.reset();
    }


}
