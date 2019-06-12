package com.flightstats.hub.channel;

import com.flightstats.hub.dao.ContentDaoUtil;
import com.flightstats.hub.model.Content;
import com.flightstats.hub.model.ContentKey;
import com.google.common.io.ByteStreams;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
public class ZipBulkBuilderTest {

    @Test
    public void testCycle() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ZipOutputStream output = new ZipOutputStream(baos);
        ContentKey key = new ContentKey();
        Content content = ContentDaoUtil.createContent(key);
        ZipBulkBuilder zipBulkBuilder = new ZipBulkBuilder();
        zipBulkBuilder.createZipEntry(output, content);
        output.close();
        byte[] bytes = baos.toByteArray();
        log.info("wrote bytes {}", bytes.length);
        ZipInputStream zipInputStream = new ZipInputStream(new ByteArrayInputStream(bytes));
        ZipEntry entry = zipInputStream.getNextEntry();
        while (entry != null) {
            String name = entry.getName();
            String comment = new String(entry.getExtra());
            byte[] readBytes = ByteStreams.toByteArray(zipInputStream);
            assertEquals(key.toUrl(), name);
            assertEquals("{\"contentType\":\"stuff\"}", comment);
            assertEquals(key.toUrl(), new String(readBytes));
            entry = zipInputStream.getNextEntry();
        }
    }

}