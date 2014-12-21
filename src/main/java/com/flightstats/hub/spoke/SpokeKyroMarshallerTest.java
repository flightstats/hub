package com.flightstats.hub.spoke;

import com.flightstats.hub.model.Content2;
import com.flightstats.hub.model.ContentKey;
import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class SpokeKyroMarshallerTest {

    private final static Logger logger = LoggerFactory.getLogger(SpokeKyroMarshallerTest.class);
    public static final int WARM_UP = 10 * 1000;
    public static final int LOOPS = 1 * 1000;
    public static final int DATA = 1000 * 1024;

    public static void main(String[] args) throws IOException {
        Content2 content = getContent2(20);
        Content2 cycled = SpokeKyroMarshaller.toContent(SpokeKyroMarshaller.toBytes(content), content.getContentKey());
        if (!cycled.equals(content)) {
            logger.warn("inequal {} {}", content, cycled);
        }
        for (int i = 0; i < WARM_UP; i++) {
            byte[] bytes = SpokeKyroMarshaller.toBytes(content);
        }
        content = getContent2(DATA);
        logger.info("random bytes {} ", content.getData().length);
        logger.info("marshalled bytes {} ", SpokeKyroMarshaller.toBytes(content).length);
        long start = System.currentTimeMillis();
        for (int i = 0; i < LOOPS; i++) {
            byte[] bytes = SpokeKyroMarshaller.toBytes(content);
        }
        logger.info("completed {} {}", LOOPS, (System.currentTimeMillis() - start));

    }

/*    private static Content2 getContent2() throws IOException {
        byte[] bytes = IOUtils.toByteArray(new FileInputStream("/Users/gmoulliet/code/hubv2/src/main/resources/positionsAsdi.json"));
        return Content2.builder()
                .withContentType("application/json")
                .withContentKey(new ContentKey())
                .withData(bytes)
                .build();
    }*/

    private static Content2 getContent2(int size) throws IOException {
        String random = RandomStringUtils.randomAlphanumeric(size);
        //String random = StringUtils.repeat("A", size);
        return Content2.builder()
                .withContentType("application/json")
                .withContentKey(new ContentKey())
                .withData(random.getBytes())
                .build();
    }
}