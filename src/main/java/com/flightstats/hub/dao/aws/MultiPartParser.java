package com.flightstats.hub.dao.aws;

import com.flightstats.hub.exception.ContentTooLargeException;
import com.flightstats.hub.exception.InvalidRequestException;
import com.flightstats.hub.model.BulkContent;
import com.flightstats.hub.model.Content;
import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.util.ByteRing;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Slf4j
public class MultiPartParser {

    private static final byte[] CRLF = "\r\n".getBytes();
    private final ByteArrayOutputStream baos;
    private BulkContent bulkContent;
    private BufferedInputStream stream;
    private Content.Builder builder;
    private final int maxBytes;

    public MultiPartParser(BulkContent bulkContent, int maxPayloadSizeInMB) {
        this.bulkContent = bulkContent;
        builder = Content.builder();
        stream = new BufferedInputStream(bulkContent.getStream());
        baos = new ByteArrayOutputStream();
        this.maxBytes = maxPayloadSizeInMB  * 1024 * 1024 * 3;
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
                log.warn("multipart max payload exceeded {}", maxBytes, bulkContent.getChannel());
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
