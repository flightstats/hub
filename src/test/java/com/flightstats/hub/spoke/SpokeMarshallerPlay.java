package com.flightstats.hub.spoke;

import com.flightstats.hub.model.Content;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;

public class SpokeMarshallerPlay {

    private final static Logger logger = LoggerFactory.getLogger(SpokeMarshallerPlay.class);
    public static final int WARM_UP = 10 * 1000;
    public static final int LOOPS = 10 * 1000;

    public static void main(String[] args) throws IOException {
        Content content = getContent(1024);
        for (int i = 0; i < WARM_UP; i++) {
            byte[] bytes = SpokeMarshaller.toBytes(content);
        }
        content = getContent(10 * 1024);
        logger.info("random bytes {} ", content.getData().length);
        logger.info("marshalled bytes {} ", SpokeMarshaller.toBytes(content).length);
        long start = System.currentTimeMillis();
        for (int i = 0; i < LOOPS; i++) {
            byte[] bytes = SpokeMarshaller.toBytes(content);
        }
        logger.info("completed {} {}", LOOPS, (System.currentTimeMillis() - start));

    }

    private static Content getContent(int size) throws IOException {
        byte[] bytes = IOUtils.toByteArray(new FileInputStream("/Users/gmoulliet/code/hubv2/src/main/resources/positionsAsdi.json"));
        //String random = RandomStringUtils.randomAlphanumeric(size);
        return Content.builder()
                .withContentType("application/json")
                .withData(bytes)
                        //.withData(random.getBytes())
                .build();
    }
}
