package com.flightstats.hub.spoke;

import com.flightstats.hub.model.Content;
import com.flightstats.hub.model.ContentKey;
import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class KyroMarshallerPlay {

    private final static Logger logger = LoggerFactory.getLogger(KyroMarshallerPlay.class);
    public static final int WARM_UP = 10 * 1000;
    public static final int LOOPS = 100 * 1000;
    public static final int DATA = 40 * 1024;

    public static void main(String[] args) throws IOException {
        Content content = getContent(20);
        Content cycled = SpokeKyroMarshaller.toContent(SpokeKyroMarshaller.toBytes(content), content.getContentKey().get());
        if (!cycled.equals(content)) {
            logger.warn("inequal {} {}", content, cycled);
        }
        for (int i = 0; i < WARM_UP; i++) {
            byte[] bytes = SpokeKyroMarshaller.toBytes(content);
        }
        content = getContent(DATA);
        logger.info("data bytes {} ", content.getData().length);
        logger.info("marshalled bytes {} ", SpokeKyroMarshaller.toBytes(content).length);
        long start = System.currentTimeMillis();
        for (int i = 0; i < LOOPS; i++) {
            byte[] bytes = SpokeKyroMarshaller.toBytes(content);
        }
        logger.info("completed {} {}", LOOPS, (System.currentTimeMillis() - start));

    }

/*    private static Content getContent() throws IOException {
        byte[] bytes = IOUtils.toByteArray(new FileInputStream("/Users/gmoulliet/code/hubv2/src/main/resources/positionsAsdi.json"));
        return Content.builder()
                .withContentType("application/json")
                .withContentKey(new ContentKey())
                .withData(bytes)
                .build();
    }*/

    private static Content getContent(int size) throws IOException {
        String data = "";
        String random = RandomStringUtils.randomAlphanumeric(6 * 1204);
        while (data.length() < size) {
            data += random;
        }

        return Content.builder()
                .withContentType("application/json")
                .withContentKey(new ContentKey())
                .withData(data.getBytes())
                .build();
    }
}