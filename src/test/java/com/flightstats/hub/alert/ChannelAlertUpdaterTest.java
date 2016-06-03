package com.flightstats.hub.alert;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.util.TimeUtil;
import org.joda.time.DateTime;
import org.junit.AfterClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import static com.flightstats.hub.test.SparkUtil.*;
import static org.junit.Assert.*;

public class ChannelAlertUpdaterTest {

    private final static Logger logger = LoggerFactory.getLogger(ChannelAlertUpdaterTest.class);
    private final static ObjectMapper mapper = new ObjectMapper();
    private static final String HUB_DOMAIN = "http://localhost:4567/";
    private static final List<String> alerts = new ArrayList<>();

    @AfterClass
    public static void tearDownClass() throws Exception {
        stop();
    }

    @Test
    public void testMinutesNew() throws Exception {
        AlertConfig config = createConfig("testMinutesNew", ">", 16, 5);
        configureSpark("testMinutesNew", TimeUtil.Unit.MINUTES, 5);
        ChannelAlertUpdater channelAlertUpdater = new ChannelAlertUpdater(config, null);
        AlertStatus alertStatus = channelAlertUpdater.call();
        assertEquals(5, alertStatus.getHistory().size());
        assertFalse(alertStatus.isAlert());
        assertEquals(0, alerts.size());
    }

    private void configureSpark(String channel, TimeUtil.Unit unit, int count) {
        alerts.clear();
        DateTime time = TimeUtil.now();
        final DateTime startTime = time;
        get("/channel/" + channel + "/time/" + unit.getName(), (req, res) -> {
            res.redirect("/channel/" + channel + "/" + unit.format(startTime));
            return "";
        });
        post("/channel/escalationAlerts", (req, res) -> {
            alerts.add(req.body());
            return "";
        });
        for (int i = 0; i <= count; i++) {
            final int counter = i;
            String path = "/channel/" + channel + "/" + unit.format(time);
            logger.info("setting up path {} as {}", counter, path);
            final DateTime finalTime = time;
            get(path, (req, res) -> {
                        ObjectNode rootNode = mapper.createObjectNode();
                        ObjectNode links = rootNode.putObject("_links");
                        links.putObject("self").put("href", HUB_DOMAIN + "channel/" + channel + "/" + unit.format(finalTime));
                        links.putObject("previous").put("href", HUB_DOMAIN + "channel/" + channel + "/" + unit.format(finalTime.minus(unit.getDuration())));
                        if (counter > 0) {
                            links.putObject("next").put("href", HUB_DOMAIN + "channel/" + channel + "/" + unit.format(finalTime.plus(unit.getDuration())));
                        }
                        ArrayNode uris = links.putArray("uris");
                        for (int j = 0; j < counter; j++) {
                            uris.add(HUB_DOMAIN + "channel/" + channel + "/" + new ContentKey().toUrl());
                        }
                        return rootNode.toString();
                    }
            );
            time = time.minus(unit.getDuration());
        }
    }

    @Test
    public void testHoursNew() throws Exception {
        AlertConfig config = createConfig("testHoursNew", "==", 6, 125);
        configureSpark("testHoursNew", TimeUtil.Unit.HOURS, 3);
        ChannelAlertUpdater channelAlertUpdater = new ChannelAlertUpdater(config, null);
        AlertStatus alertStatus = channelAlertUpdater.call();

        System.out.println(alertStatus);
        assertEquals(3, alertStatus.getHistory().size());
        assertTrue(alertStatus.isAlert());
        assertEquals(1, alerts.size());
    }

    @Test
    public void testHoursPrevious() throws Exception {
        String channel = "testHoursPrevious";
        AlertConfig config = createConfig(channel, "==", 61, 240);
        AlertStatus status = AlertStatus.builder()
                .period("hour")
                .name("stuff")
                .alert(false)
                .history(createHistory(TimeUtil.Unit.HOURS, 4, channel))
                .build();
        configureSpark(channel, TimeUtil.Unit.HOURS, 3);
        ChannelAlertUpdater channelAlertUpdater = new ChannelAlertUpdater(config, status);
        AlertStatus alertStatus = channelAlertUpdater.call();
        LinkedList<AlertStatusHistory> histories = alertStatus.getHistory();
        for (AlertStatusHistory statusHistory : histories) {
            System.out.println(statusHistory);
        }
        assertEquals(4, histories.size());
        assertTrue(alertStatus.isAlert());
        assertEquals(1, alerts.size());
    }

    @Test
    public void testMinutesPrevious() throws Exception {
        String channel = "testMinutesPrevious";
        AlertConfig config = createConfig(channel, "==", 31, 3);
        AlertStatus status = AlertStatus.builder()
                .period("minute")
                .name("stuff")
                .alert(false)
                .history(createHistory(TimeUtil.Unit.MINUTES, 3, channel))
                .build();
        configureSpark(channel, TimeUtil.Unit.MINUTES, 3);
        ChannelAlertUpdater channelAlertUpdater = new ChannelAlertUpdater(config, status);
        AlertStatus alertStatus = channelAlertUpdater.call();
        LinkedList<AlertStatusHistory> histories = alertStatus.getHistory();
        for (AlertStatusHistory statusHistory : histories) {
            System.out.println(statusHistory);
        }
        assertEquals(3, histories.size());
        assertTrue(alertStatus.isAlert());
        assertEquals(1, alerts.size());
    }

    @Test
    public void testMinutesToHours() throws Exception {
        String channel = "testMinutesToHours";
        AlertConfig config = createConfig("testMinutesToHours", ">", 0, 125);

        AlertStatus status = AlertStatus.builder()
                .period("minute")
                .name("stuff")
                .alert(false)
                .history(createHistory(TimeUtil.Unit.HOURS, 90, channel))
                .build();
        configureSpark(channel, TimeUtil.Unit.HOURS, 3);
        ChannelAlertUpdater channelAlertUpdater = new ChannelAlertUpdater(config, status);
        AlertStatus alertStatus = channelAlertUpdater.call();
        LinkedList<AlertStatusHistory> histories = alertStatus.getHistory();
        for (AlertStatusHistory statusHistory : histories) {
            System.out.println(statusHistory);
        }
        assertEquals(3, histories.size());
        assertEquals(1, alerts.size());
    }

    @Test
    public void testMinutesShorten() throws Exception {
        String channel = "testMinutesShorten";
        AlertConfig config = createConfig(channel, "==", 171, 3);
        AlertStatus status = AlertStatus.builder()
                .period("minute")
                .name("stuff")
                .alert(false)
                .history(createHistory(TimeUtil.Unit.MINUTES, 10, channel))
                .build();
        configureSpark(channel, TimeUtil.Unit.MINUTES, 3);
        ChannelAlertUpdater channelAlertUpdater = new ChannelAlertUpdater(config, status);
        AlertStatus alertStatus = channelAlertUpdater.call();
        LinkedList<AlertStatusHistory> histories = alertStatus.getHistory();
        for (AlertStatusHistory statusHistory : histories) {
            System.out.println(statusHistory);
        }
        assertEquals(3, histories.size());
        assertTrue(alertStatus.isAlert());
        assertEquals(1, alerts.size());
    }

    private AlertConfig createConfig(String channel, String operator, int threshold, int timeWindowMinutes) {
        return AlertConfig.builder()
                .hubDomain(HUB_DOMAIN)
                .name(channel)
                .channel(channel)
                .operator(operator)
                .threshold(threshold)
                .timeWindowMinutes(timeWindowMinutes)
                .build();
    }

    private LinkedList<AlertStatusHistory> createHistory(TimeUtil.Unit unit, int count, String channel) {
        DateTime now = TimeUtil.now();
        logger.info("now {}", unit.format(now));
        DateTime time = now.minus(unit.getDuration().multipliedBy(count));

        LinkedList<AlertStatusHistory> history = new LinkedList<>();
        for (int i = 1; i < count; i++) {
            AlertStatusHistory alertStatusHistory = AlertStatusHistory.builder()
                    .items(i * 10)
                    .href(HUB_DOMAIN + "channel/" + channel + "/" + unit.format(time))
                    .build();
            logger.info("adding {}", alertStatusHistory);
            time = time.plus(unit.getDuration());
            history.add(alertStatusHistory);
        }
        return history;
    }

}