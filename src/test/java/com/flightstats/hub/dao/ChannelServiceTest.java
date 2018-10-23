package com.flightstats.hub.dao;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flightstats.hub.app.HubModule;
import com.flightstats.hub.app.HubProperties;
import com.flightstats.hub.app.InFlightService;
import com.flightstats.hub.channel.ChannelValidator;
import com.flightstats.hub.cluster.LastContentPath;
import com.flightstats.hub.exception.ConflictException;
import com.flightstats.hub.metrics.MetricsService;
import com.flightstats.hub.model.ChannelConfig;
import com.flightstats.hub.replication.ReplicationManager;
import com.flightstats.hub.time.TimeService;
import com.flightstats.hub.util.TimeUtil;
import com.google.gson.Gson;
import org.joda.time.DateTime;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ChannelServiceTest {

    @Test(expected = ConflictException.class)
    public void testVerifyChannelUniqueness() {
        Dao channelConfigDao = mock(Dao.class);
        ContentService contentService = mock(ContentService.class);
        ChannelValidator channelValidator = mock(ChannelValidator.class);
        ReplicationManager replicationManager = mock(ReplicationManager.class);
        LastContentPath lastContentPath = mock(LastContentPath.class);
        InFlightService inFlightService = mock(InFlightService.class);
        TimeService timeService = mock(TimeService.class);
        MetricsService metricsService = mock(MetricsService.class);
        HubProperties hubProperties = mock(HubProperties.class);
        ObjectMapper mapper = mock(ObjectMapper.class);
        Gson gson = new Gson();
        ChannelService channelService = new ChannelService(channelConfigDao, contentService, channelValidator, replicationManager, lastContentPath, inFlightService, timeService, metricsService, hubProperties, mapper, gson);

        String channelName = "test";
        when(channelService.channelExists(channelName)).thenReturn(true);
        ChannelConfig channelConfig = ChannelConfig.builder().name(channelName).build();
        channelService.verifyChannelUniqueness(channelConfig);
    }

    @Test
    public void testCreateFromJson() {
        Dao channelConfigDao = mock(Dao.class);
        ContentService contentService = mock(ContentService.class);
        ChannelValidator channelValidator = mock(ChannelValidator.class);
        ReplicationManager replicationManager = mock(ReplicationManager.class);
        LastContentPath lastContentPath = mock(LastContentPath.class);
        InFlightService inFlightService = mock(InFlightService.class);
        TimeService timeService = mock(TimeService.class);
        MetricsService metricsService = mock(MetricsService.class);
        HubProperties hubProperties = mock(HubProperties.class);
        ObjectMapper mapper = mock(ObjectMapper.class);
        Gson gson = mock(Gson.class);
        ChannelService channelService = new ChannelService(channelConfigDao, contentService, channelValidator, replicationManager, lastContentPath, inFlightService, timeService, metricsService, hubProperties, mapper, gson);

        ChannelConfig config = channelService.createFromJson("{\"name\": \"defaults\"}");

        assertEquals("defaults", config.getName());
        assertEquals(120, config.getTtlDays());
        assertEquals("", config.getDescription());
        assertTrue(config.getTags().isEmpty());
        assertEquals("", config.getReplicationSource());
        assertEquals("SINGLE", config.getStorage());
        assertNull(config.getMutableTime());
        assertTrue(config.isAllowZeroBytes());
    }

    @Test
    public void testSerializeToJsonAndBack() throws IOException {
        Dao channelConfigDao = mock(Dao.class);
        ContentService contentService = mock(ContentService.class);
        ChannelValidator channelValidator = mock(ChannelValidator.class);
        ReplicationManager replicationManager = mock(ReplicationManager.class);
        LastContentPath lastContentPath = mock(LastContentPath.class);
        InFlightService inFlightService = mock(InFlightService.class);
        TimeService timeService = mock(TimeService.class);
        MetricsService metricsService = mock(MetricsService.class);
        HubProperties hubProperties = mock(HubProperties.class);
        ObjectMapper mapper = mock(ObjectMapper.class);
        HubModule hubModule = new HubModule(new Properties());
        Gson gson = hubModule.provideGson();
        ChannelService channelService = new ChannelService(channelConfigDao, contentService, channelValidator, replicationManager, lastContentPath, inFlightService, timeService, metricsService, hubProperties, mapper, gson);

        ChannelConfig config = ChannelConfig.builder()
                .owner("ABC")
                .description("something something")
                .ttlDays(15)
                .maxItems(5)
                .tags(Arrays.asList("uno", "dos"))
                .replicationSource("theSources")
                .storage("whyNotEnum?")
                .protect(false)
                .mutableTime(TimeUtil.now())
                .allowZeroBytes(false)
                .build();

        assertEquals(config, channelService.createFromJson(gson.toJson(config)));
    }

    @Test
    public void testUpdateFromJsonIsLossless() throws IOException {
        Dao channelConfigDao = mock(Dao.class);
        ContentService contentService = mock(ContentService.class);
        ChannelValidator channelValidator = mock(ChannelValidator.class);
        ReplicationManager replicationManager = mock(ReplicationManager.class);
        LastContentPath lastContentPath = mock(LastContentPath.class);
        InFlightService inFlightService = mock(InFlightService.class);
        TimeService timeService = mock(TimeService.class);
        MetricsService metricsService = mock(MetricsService.class);
        HubProperties hubProperties = mock(HubProperties.class);
        ObjectMapper mapper = mock(ObjectMapper.class);
        HubModule hubModule = new HubModule(new Properties());
        Gson gson = hubModule.provideGson();
        ChannelService channelService = new ChannelService(channelConfigDao, contentService, channelValidator, replicationManager, lastContentPath, inFlightService, timeService, metricsService, hubProperties, mapper, gson);

        DateTime mutableTime = DateTime.now();
        ChannelConfig config = ChannelConfig.builder()
                .owner("ABC")
                .description("something something")
                .ttlDays(15)
                .maxItems(5)
                .tags(Arrays.asList("uno", "dos"))
                .replicationSource("theSources")
                .storage("whyNotEnum?")
                .protect(false)
                .allowZeroBytes(false)
                .mutableTime(mutableTime)
                .build();

        ChannelConfig config2 = ChannelConfig.builder()
                .owner("ABC")
                .description("something something")
                .keepForever(true)
                .ttlDays(15)
                .build();

        ChannelConfig updated = channelService.updateFromJson(config, gson.toJson(config2));

        assertEquals(0, updated.getMaxItems());
        assertEquals(0, updated.getTtlDays());
        assertEquals(mutableTime, updated.getMutableTime());
    }

}
