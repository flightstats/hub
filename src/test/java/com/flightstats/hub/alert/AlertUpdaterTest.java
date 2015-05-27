package com.flightstats.hub.alert;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.util.TimeUtil;
import org.jetbrains.annotations.NotNull;
import org.joda.time.DateTime;
import org.junit.AfterClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;

import static com.flightstats.hub.test.SparkUtil.get;
import static com.flightstats.hub.test.SparkUtil.stop;
import static org.junit.Assert.*;

public class AlertUpdaterTest {

    private final static Logger logger = LoggerFactory.getLogger(AlertUpdaterTest.class);
    private final static ObjectMapper mapper = new ObjectMapper();
    public static final String HUB_DOMAIN = "http://localhost:4567/";

    @AfterClass
    public static void tearDownClass() throws Exception {
        stop();
    }

    @Test
    public void testMinutesNew() throws Exception {
        AlertConfig config = AlertConfig.builder()
                .hubDomain(HUB_DOMAIN)
                .name("simple")
                .channel("testMinutesNew")
                .operator(">")
                .threshold(16)
                .timeWindowMinutes(5)
                .build();

        configureSpark("testMinutesNew", TimeUtil.Unit.MINUTES, 5);
        AlertUpdater alertUpdater = new AlertUpdater(config, null);
        AlertStatus alertStatus = alertUpdater.call();
        System.out.println(alertStatus);
        assertEquals(5, alertStatus.getHistory().size());
        assertFalse(alertStatus.isAlert());
    }

    private void configureSpark(String channel, TimeUtil.Unit unit, int count) {
        DateTime time = TimeUtil.now();
        final DateTime startTime = time;
        get("/channel/" + channel + "/time/" + unit.getName(), (req, res) -> {
            res.redirect("/channel/" + channel + "/" + unit.format(startTime));
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
        AlertConfig config = AlertConfig.builder()
                .hubDomain(HUB_DOMAIN)
                .name("testHoursNew")
                .channel("testHoursNew")
                .operator("==")
                .threshold(6)
                .timeWindowMinutes(125)
                .build();

        configureSpark("testHoursNew", TimeUtil.Unit.HOURS, 3);
        AlertUpdater alertUpdater = new AlertUpdater(config, null);
        AlertStatus alertStatus = alertUpdater.call();

        System.out.println(alertStatus);
        assertEquals(3, alertStatus.getHistory().size());
        assertTrue(alertStatus.isAlert());
    }

    @Test
    public void testHoursPrevious() throws Exception {
        String channel = "testHoursPrevious";
        AlertConfig config = AlertConfig.builder()
                .hubDomain(HUB_DOMAIN)
                .name(channel)
                .channel(channel)
                .operator("==")
                .threshold(61)
                .timeWindowMinutes(240)
                .build();

        AlertStatus status = AlertStatus.builder()
                .period("hour")
                .name("stuff")
                .alert(false)
                .history(createHistory(TimeUtil.Unit.HOURS, 4, channel))
                .build();
        System.out.println("before " + status);
        configureSpark(channel, TimeUtil.Unit.HOURS, 3);
        AlertUpdater alertUpdater = new AlertUpdater(config, status);
        AlertStatus alertStatus = alertUpdater.call();

        System.out.println("after " + alertStatus);
        LinkedList<AlertStatusHistory> histories = alertStatus.getHistory();
        for (AlertStatusHistory statusHistory : histories) {
            System.out.println(statusHistory);
        }
        assertEquals(4, histories.size());
        assertTrue(alertStatus.isAlert());
    }

    @Test
    public void testMinutesPrevious() throws Exception {
        String channel = "testMinutesPrevious";
        AlertConfig config = AlertConfig.builder()
                .hubDomain(HUB_DOMAIN)
                .name(channel)
                .channel(channel)
                .operator("==")
                .threshold(31)
                .timeWindowMinutes(3)
                .build();

        AlertStatus status = AlertStatus.builder()
                .period("minute")
                .name("stuff")
                .alert(false)
                .history(createHistory(TimeUtil.Unit.MINUTES, 3, channel))
                .build();
        System.out.println("before " + status);
        configureSpark(channel, TimeUtil.Unit.MINUTES, 3);
        AlertUpdater alertUpdater = new AlertUpdater(config, status);
        AlertStatus alertStatus = alertUpdater.call();

        System.out.println("after " + alertStatus);
        LinkedList<AlertStatusHistory> histories = alertStatus.getHistory();
        for (AlertStatusHistory statusHistory : histories) {
            System.out.println(statusHistory);
        }
        assertEquals(3, histories.size());
        assertTrue(alertStatus.isAlert());
    }

    @Test
    public void testMinutesToHours() throws Exception {
        String channel = "testMinutesToHours";
        AlertConfig config = AlertConfig.builder()
                .hubDomain(HUB_DOMAIN)
                .name("testMinutesToHours")
                .channel(channel)
                .operator(">")
                .threshold(0)
                .timeWindowMinutes(125)
                .build();

        AlertStatus status = AlertStatus.builder()
                .period("minute")
                .name("stuff")
                .alert(false)
                .history(createHistory(TimeUtil.Unit.HOURS, 90, channel))
                .build();
        System.out.println("before " + status);
        configureSpark(channel, TimeUtil.Unit.HOURS, 3);
        AlertUpdater alertUpdater = new AlertUpdater(config, status);
        AlertStatus alertStatus = alertUpdater.call();

        System.out.println("after " + alertStatus);
        LinkedList<AlertStatusHistory> histories = alertStatus.getHistory();
        for (AlertStatusHistory statusHistory : histories) {
            System.out.println(statusHistory);
        }
        assertEquals(3, histories.size());
    }

    @NotNull
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