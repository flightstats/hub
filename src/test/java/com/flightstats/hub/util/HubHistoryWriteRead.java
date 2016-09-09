package com.flightstats.hub.util;

import com.flightstats.hub.app.HubBindings;
import com.flightstats.hub.model.ChannelConfig;
import com.flightstats.hub.model.Content;
import com.flightstats.hub.model.ContentKey;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.MediaType;
import java.util.Arrays;

public class HubHistoryWriteRead {

    private final static Logger logger = LoggerFactory.getLogger(HubHistoryWriteRead.class);
    private static final byte[] BYTES = "alot".getBytes();
    private static HubUtils hubUtils;

    public static void main(String[] args) {

        String channelUrl = "http://hub.iad.dev.flightstats.io/channel/historyReadWrite4";
        //String channelUrl = "http://localhost:9080/channel/historyReadWrite";
        hubUtils = new HubUtils(HubBindings.buildJerseyClientNoRedirects(), HubBindings.buildJerseyClient());
        /*

        * create a historical channel
         * writes historical items into channel,
         * and then reads those item back out
         * verifying that the patload is the same size
         */
        hubUtils.delete(channelUrl);
        ChannelConfig config = ChannelConfig.builder()
                .withHistorical(true)
                .withTtlDays(3650)
                .build();
        hubUtils.putChannel(channelUrl, config);
        DateTime time = new DateTime(2007, 8, 24, 2, 0, 0, 0);
        int count = 0;
        while (true) {
            count++;
            time = time.plusHours(1);
            insertAndVerify(channelUrl, time);
            //Sleeper.sleep(10);
            if (count % 100 == 0) {
                logger.warn("count " + count);
            }
        }
    }

    private static void insertAndVerify(String channelUrl, DateTime startTime) {
        Content content = Content.builder()
                .withContentType(MediaType.TEXT_PLAIN)
                .withData(BYTES)
                .build();
        String url = channelUrl + "/" + TimeUtil.millis(startTime);
        ContentKey insert = hubUtils.insert(url, content);
        if (insert == null) {
            logger.info("got back null {}", url);
        }
        //verify(channelUrl, insert);
        //verify(channelUrl, insert);
    }

    private static void verify(String channelUrl, ContentKey insert) {
        Content found = hubUtils.get(channelUrl, insert);
        if (found == null) {
            logger.warn("found null data for key {}", insert);

        } else {
            byte[] data = found.getData();
            if (!Arrays.equals(BYTES, data)) {
                logger.warn("unequal data key={} data='{}'", insert, new String(data));
            }
        }
    }
}
