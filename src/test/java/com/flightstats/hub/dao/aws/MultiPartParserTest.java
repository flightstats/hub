package com.flightstats.hub.dao.aws;

import com.flightstats.hub.model.BatchContent;
import com.flightstats.hub.model.Content;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MultiPartParserTest {

    @Test
    public void testSimple() throws IOException {
        String data = "This is a message with multiple parts in MIME format.\r\n" +
                "--frontier\r\n" +
                "Content-Type: text/plain\r\n" +
                " \r\n" +
                "This is the body of the message.\r\n" +
                "--frontier\r\n" +
                "Content-Type: application/octet-stream\r\n" +
                "Content-Transfer-Encoding: base64\r\n" +
                "\r\n" +
                "PGh0bWw+CiAgPGhlYWQ+CiAgPC9oZWFkPgogIDxib2R5PgogICAgPHA+VGhpcyBpcyB0aGUgYm9keSBvZiB0aGUgbWVzc2FnZS48L3A+CiAgPC9ib2R5Pgo8L2h0bWw+Cg==\r\n" +
                "--frontier--";

        ByteArrayInputStream inputStream = new ByteArrayInputStream(data.getBytes());
        BatchContent batchContent = BatchContent.builder()
                .withStream(inputStream)
                .withContentType("multipart/mixed; boundary=frontier")
                .build();
        MultiPartParser parser = new MultiPartParser(batchContent);
        parser.parse();
        DateTime time = new DateTime();
        Content item = batchContent.getItems().get(0);
        assertEquals("This is the body of the message.", new String(item.getData()));
        assertEquals("text/plain", item.getContentType().get());
        assertTrue(item.getContentKey().get().getTime().isBefore(time));
        assertTrue(StringUtils.endsWith(item.getContentKey().get().getHash(), "000000"));
        item = batchContent.getItems().get(1);
        assertEquals("PGh0bWw+CiAgPGhlYWQ+CiAgPC9oZWFkPgogIDxib2R5PgogICAgPHA+VGhpcyBpcyB0aGUgYm9keSBvZiB0aGUgbWVzc2FnZS48L3A+CiAgPC9ib2R5Pgo8L2h0bWw+Cg==", new String(item.getData()));
        assertEquals("application/octet-stream", item.getContentType().get());
        assertTrue(item.getContentKey().get().getTime().isBefore(time));
        assertTrue(StringUtils.endsWith(item.getContentKey().get().getHash(), "000001"));
    }

    @Test
    public void testMinimal() throws IOException {
        String data = "--boundary\r\n" +
                "\r\n" +
                "There is some message here.\r\n" +
                "--boundary--";

        ByteArrayInputStream inputStream = new ByteArrayInputStream(data.getBytes());
        BatchContent batchContent = BatchContent.builder()
                .withStream(inputStream)
                .withContentType("multipart/mixed; boundary=boundary")
                .build();
        MultiPartParser parser = new MultiPartParser(batchContent);
        parser.parse();
        Content item = batchContent.getItems().get(0);
        assertEquals("There is some message here.", new String(item.getData()));
        assertEquals("text/plain", item.getContentType().get());
    }

}