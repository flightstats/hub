package com.flightstats.hub.channel;

import com.flightstats.hub.dao.ContentDaoUtil;
import com.flightstats.hub.model.Content;
import com.flightstats.hub.model.ContentKey;
import com.google.common.io.ByteStreams;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import static org.junit.Assert.assertEquals;

public class ZipBatchBuilderTest {

    private final static Logger logger = LoggerFactory.getLogger(ZipBatchBuilderTest.class);

    @Test
    public void testCycle() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ZipOutputStream output = new ZipOutputStream(baos);
        ContentKey key = new ContentKey();
        Content content = ContentDaoUtil.createContent(key);
        ZipBatchBuilder.createZipEntry(output, key, content);
        output.close();
        byte[] bytes = baos.toByteArray();
        logger.info("wrote bytes {}", bytes.length);
        ZipInputStream zipInputStream = new ZipInputStream(new ByteArrayInputStream(bytes));
        ZipEntry entry = zipInputStream.getNextEntry();
        int count = 0;
        while (entry != null) {
            count++;
            String name = entry.getName();
            String comment = new String(entry.getExtra());
            byte[] readBytes = ByteStreams.toByteArray(zipInputStream);
            assertEquals(key.toUrl(), name);
            assertEquals("{\"contentLanguage\":\"lang\",\"contentType\":\"stuff\"}", comment);
            assertEquals(key.toUrl(), new String(readBytes));
            entry = zipInputStream.getNextEntry();
        }
    }

}