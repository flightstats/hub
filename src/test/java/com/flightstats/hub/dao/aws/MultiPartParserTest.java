package com.flightstats.hub.dao.aws;

import com.flightstats.hub.model.BulkContent;
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
        BulkContent bulkContent = BulkContent.builder()
                .stream(inputStream)
                .contentType("multipart/mixed; boundary=frontier")
                .build();
        MultiPartParser parser = new MultiPartParser(bulkContent);
        parser.parse();
        Content item = bulkContent.getItems().get(0);
        assertEquals("This is the body of the message.", new String(item.getData()));
        assertEquals("text/plain", item.getContentType().get());
        DateTime time = new DateTime().plusMillis(1);
        assertTrue(item.getContentKey().get().getTime().isBefore(time));
        assertTrue(StringUtils.endsWith(item.getContentKey().get().getHash(), "000000"));
        item = bulkContent.getItems().get(1);
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
        BulkContent bulkContent = BulkContent.builder()
                .stream(inputStream)
                .contentType("multipart/mixed; boundary=boundary")
                .build();
        MultiPartParser parser = new MultiPartParser(bulkContent);
        parser.parse();
        Content item = bulkContent.getItems().get(0);
        assertEquals("There is some message here.", new String(item.getData()));
        assertEquals("text/plain", item.getContentType().get());
    }

    @Test
    public void testContentHeaders() throws IOException {
        String data = "--boundary\r\n" +
                "Content-Transfer-Encoding: text\r\n" +
                "Content-Type: application/ocelot-stream\r\n" +
                "\r\n" +
                "meow.\r\n" +
                "--boundary--";

        ByteArrayInputStream inputStream = new ByteArrayInputStream(data.getBytes());
        BulkContent bulkContent = BulkContent.builder()
                .stream(inputStream)
                .contentType("multipart/mixed; boundary=\"boundary\"")
                .build();
        MultiPartParser parser = new MultiPartParser(bulkContent);
        parser.parse();
        Content item = bulkContent.getItems().get(0);
        assertEquals("meow.", new String(item.getData()));
        assertEquals("application/ocelot-stream", item.getContentType().get());
    }

    @Test
    public void testWhiteSpace() throws IOException {
        String data = "--boundary\r\n" +
                "\r\n" +
                "\r\n" +
                "There is some message here.\r\n" +
                "\r\n" +
                "--boundary--";

        ByteArrayInputStream inputStream = new ByteArrayInputStream(data.getBytes());
        BulkContent bulkContent = BulkContent.builder()
                .stream(inputStream)
                .contentType("multipart/mixed; boundary=boundary")
                .build();
        MultiPartParser parser = new MultiPartParser(bulkContent);
        parser.parse();
        Content item = bulkContent.getItems().get(0);
        assertEquals("\r\nThere is some message here.\r\n", new String(item.getData()));
        assertEquals("text/plain", item.getContentType().get());
    }

}