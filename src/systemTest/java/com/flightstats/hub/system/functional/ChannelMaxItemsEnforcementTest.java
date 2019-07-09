package com.flightstats.hub.system.functional;

import com.flightstats.hub.model.ChannelConfig;
import com.flightstats.hub.system.extension.TestClassWrapper;
import com.flightstats.hub.system.service.ChannelConfigService;
import com.flightstats.hub.system.service.ChannelItemCreator;
import com.flightstats.hub.system.service.ChannelItemRetriever;
import com.flightstats.hub.system.service.HubInternalService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static com.flightstats.hub.util.StringUtils.randomAlphaNumeric;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChannelMaxItemsEnforcementTest extends TestClassWrapper {
    private static final String TEST_DATA = "TEST_DATA";
    private final List<String> items = new ArrayList<>();
    private String channelName;
    @Inject
    private ChannelConfigService channelConfigService;
    @Inject
    private ChannelItemCreator channelItemCreator;
    @Inject
    private ChannelItemRetriever channelItemRetriever;
    @Inject
    private HubInternalService hubInternalService;


    @BeforeEach
    void beforeEach() {
        channelName = randomAlphaNumeric(10);
        ChannelConfig channelConfig = ChannelConfig.builder()
                .name(channelName)
                .maxItems(5)
                .build();
        channelConfigService.create(channelConfig);
        items.addAll(channelItemCreator.addItems(channelName, TEST_DATA + "initial", 6));
        hubInternalService.enforceMaxItems(channelName);
    }

    @Test
    void testAThing() {
        List<String> expected = items.subList(1, items.size());
        expected.forEach(path -> {
            Optional<Object> item = channelItemRetriever.getItem(path);
            assertTrue(item.isPresent());
        });
        Optional<Object> deleted = channelItemRetriever.getItem(items.get(0));
        assertFalse(deleted.isPresent());
    }


}
