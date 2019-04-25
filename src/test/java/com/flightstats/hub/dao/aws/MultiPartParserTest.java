package com.flightstats.hub.dao.aws;

import com.flightstats.hub.exception.InvalidRequestException;
import com.flightstats.hub.model.BulkContent;
import com.flightstats.hub.model.Content;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MultiPartParserTest {

    private static final String BINARY_ITEM = "PGh0bWw+CiAgPGhlYWQ+CiAgPC9oZWFkPgogIDxib2R5PgogICAgPHA+VGhpcyBpcyB0aGUgYm9keSBvZiB0aGUgbWVzc2FnZS48L3A+CiAgPC9ib2R5Pgo8L2h0bWw+Cg==";
    private static final int MAX_PAYLOAD_SIZE_IN_MB = 40;

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
                BINARY_ITEM + "\r\n" +
                "--frontier--";

        ByteArrayInputStream inputStream = new ByteArrayInputStream(data.getBytes());
        BulkContent bulkContent = BulkContent.builder()
                .stream(inputStream)
                .contentType("multipart/mixed; boundary=frontier")
                .isNew(true)
                .build();
        MultiPartParser parser = new MultiPartParser(bulkContent, MAX_PAYLOAD_SIZE_IN_MB);
        parser.parse();
        Content item = bulkContent.getItems().get(0);
        assertEquals("This is the body of the message.", new String(item.getData()));
        assertEquals("text/plain", item.getContentType().get());
        DateTime time = new DateTime().plusMillis(1);
        assertTrue(item.getContentKey().get().getTime().isBefore(time));
        assertTrue(StringUtils.endsWith(item.getContentKey().get().getHash(), "000000"));
        item = bulkContent.getItems().get(1);
        assertEquals(BINARY_ITEM, new String(item.getData()));
        assertEquals("application/octet-stream", item.getContentType().get());
        assertTrue(item.getContentKey().get().getTime().isBefore(time));
        assertTrue(StringUtils.endsWith(item.getContentKey().get().getHash(), "000001"));
    }

    @Test
    public void testSimpleWithKeys() throws IOException {
        String data = "This is a message with multiple parts in MIME format.\r\n" +
                "--frontier\r\n" +
                "Content-Type: text/plain\r\n" +
                "Content-Key: http://hub/channel/stumptown/2016/04/20/11/41/00/000/a\r\n" +
                "Creation-Date: 2016-04-21T05:05:05.842Z\r\n" +
                " \r\n" +
                "This is the body of the message.\r\n" +
                "--frontier\r\n" +
                "Content-Type: application/octet-stream\r\n" +
                "Content-Transfer-Encoding: base64\r\n" +
                "Content-Key: http://hub/channel/stumptown/2016/04/20/11/42/00/000/b\r\n" +
                "Creation-Date: 2016-04-21T05:05:05.842Z\r\n" +
                "\r\n" +
                BINARY_ITEM + "\r\n" +
                "--frontier--";

        ByteArrayInputStream inputStream = new ByteArrayInputStream(data.getBytes());
        BulkContent bulkContent = BulkContent.builder()
                .stream(inputStream)
                .contentType("multipart/mixed; boundary=frontier")
                .build();
        MultiPartParser parser = new MultiPartParser(bulkContent, MAX_PAYLOAD_SIZE_IN_MB);
        parser.parse();
        assertEquals(2, bulkContent.getItems().size());
        Content item = bulkContent.getItems().get(0);
        assertEquals("2016/04/20/11/41/00/000/a", item.getContentKey().get().toUrl());
        assertEquals("This is the body of the message.", new String(item.getData()));
        assertEquals("text/plain", item.getContentType().get());

        item = bulkContent.getItems().get(1);
        assertEquals(BINARY_ITEM, new String(item.getData()));
        assertEquals("application/octet-stream", item.getContentType().get());
        assertEquals("2016/04/20/11/42/00/000/b", item.getContentKey().get().toUrl());
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
        MultiPartParser parser = new MultiPartParser(bulkContent, MAX_PAYLOAD_SIZE_IN_MB);
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
        MultiPartParser parser = new MultiPartParser(bulkContent, MAX_PAYLOAD_SIZE_IN_MB);
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
        MultiPartParser parser = new MultiPartParser(bulkContent, MAX_PAYLOAD_SIZE_IN_MB);
        parser.parse();
        Content item = bulkContent.getItems().get(0);
        assertEquals("\r\nThere is some message here.\r\n", new String(item.getData()));
        assertEquals("text/plain", item.getContentType().get());
    }

    @Test(expected = InvalidRequestException.class)
    public void testMalformed() throws IOException {
        String data = "--boundary--";

        ByteArrayInputStream inputStream = new ByteArrayInputStream(data.getBytes());
        BulkContent bulkContent = BulkContent.builder()
                .stream(inputStream)
                .contentType("multipart/mixed; boundary=boundary")
                .build();
        MultiPartParser parser = new MultiPartParser(bulkContent, MAX_PAYLOAD_SIZE_IN_MB);
        parser.parse();
    }

    @Test
    public void testEmptyBytePayload() throws IOException {

        String data = "--boundary\r\n" +
                "Content-Type: text/plain\r\n" +
                "Content-Key: http://hub/channel/provider/2016/04/28/23/10/19/893/3ISEM6\r\n" +
                "Creation-Date: 2016-04-28T23:10:19.893Z\r\n" +
                "\r\n" +
                "\r\n" +
                "--boundary--";
        ByteArrayInputStream inputStream = new ByteArrayInputStream(data.getBytes());
        BulkContent bulkContent = BulkContent.builder()
                .stream(inputStream)
                .contentType("multipart/mixed; boundary=boundary")
                .build();

        MultiPartParser parser = new MultiPartParser(bulkContent, MAX_PAYLOAD_SIZE_IN_MB);
        parser.parse();
        Content item = bulkContent.getItems().get(0);
        assertArrayEquals(new byte[0], item.getData());
        assertEquals("text/plain", item.getContentType().get());

    }

}