package com.flightstats.hub.system.functional;

import com.flightstats.hub.model.ChannelConfig;
import com.flightstats.hub.model.ChannelType;
import com.flightstats.hub.model.Links;
import com.flightstats.hub.model.Location;
import com.flightstats.hub.model.TimeQueryResult;
import com.flightstats.hub.system.extension.TestClassWrapper;
import com.flightstats.hub.system.service.ChannelConfigService;
import com.flightstats.hub.system.service.ChannelItemCreator;
import com.flightstats.hub.system.service.ChannelItemRetriever;
import com.flightstats.hub.system.service.HubInternalService;
import com.flightstats.hub.system.service.S3Service;
import lombok.SneakyThrows;
import org.awaitility.Awaitility;
import org.awaitility.Duration;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import javax.inject.Inject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static com.flightstats.hub.util.StringUtils.randomAlphaNumeric;

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
    @Inject
    private S3Service s3Service;

    @SneakyThrows
    private void addItemsOverTime() {
        items.add(channelItemCreator.addItem(channelName, TEST_DATA));
        Thread.sleep(TimeUnit.MINUTES.toMillis(1));
        items.addAll(channelItemCreator.addItems(channelName, TEST_DATA, 5));
    }


    @ParameterizedTest
    @EnumSource(ChannelType.class)
    void maxItemsTest_oldestItemDeletedFromS3_itemNotInS3(ChannelType type) {
        items.clear();

        // GIVEN
        channelName = randomAlphaNumeric(10);
        ChannelConfig channelConfig = ChannelConfig.builder()
                .name(channelName)
                .storage(type.name())
                .maxItems(5)
                .build();
        channelConfigService.create(channelConfig);
        addItemsOverTime();
        String path = items.get(0);

        Awaitility.await().pollInterval(Duration.FIVE_SECONDS)
                .atMost(new Duration(3, TimeUnit.MINUTES))
                .until(() -> {
                    if (type.equals(ChannelType.BOTH)) {
                        Optional<TimeQueryResult> actualSingle = channelItemRetriever.getItemsForHourFromLocation(path, Location.LONG_TERM_SINGLE);
                        Optional<TimeQueryResult> actualBatch = channelItemRetriever.getItemsForHourFromLocation(path, Location.LONG_TERM_BATCH);

                        if (actualSingle.isPresent() && actualBatch.isPresent()) {
                            Links linksSingle = actualSingle.get().get_links();
                            Links linksBatch = actualBatch.get().get_links();
                            return Arrays.asList(linksSingle.getUris()).containsAll(items) &&
                                    Arrays.asList(linksBatch.getUris()).containsAll(items);
                        }
                        return false;
                    } else {
                        Location location = type.equals(ChannelType.BATCH) ? Location.LONG_TERM_BATCH : Location.LONG_TERM_SINGLE;
                        Optional<TimeQueryResult> actual = channelItemRetriever.getItemsForHourFromLocation(path, location);
                        if (actual.isPresent()) {
                            Links links = actual.get().get_links();
                            return Arrays.asList(links.getUris()).containsAll(items);
                        }
                        return false;
                    }
                });

        // WHEN
        hubInternalService.enforceMaxItems(channelName);


        // THEN
        Awaitility.await().pollInterval(Duration.FIVE_SECONDS)
                .atMost(Duration.TWO_MINUTES)
                .until(() -> {
                    if (type.equals(ChannelType.BOTH)) {
                        Optional<TimeQueryResult> actualSingle = channelItemRetriever.getItemsForHourFromLocation(path, Location.LONG_TERM_SINGLE);
                        Optional<TimeQueryResult> actualBatch = channelItemRetriever.getItemsForHourFromLocation(path, Location.LONG_TERM_BATCH);

                        if (actualSingle.isPresent() && actualBatch.isPresent()) {
                            Links linksSingle = actualSingle.get().get_links();
                            Links linksBatch = actualBatch.get().get_links();
                            return !Arrays.asList(linksSingle.getUris()).contains(path) &&
                                    !Arrays.asList(linksBatch.getUris()).contains(path);
                        }
                        return false;
                    } else {
                        Location location = type.equals(ChannelType.BATCH) ? Location.LONG_TERM_BATCH : Location.LONG_TERM_SINGLE;
                        Optional<TimeQueryResult> actual = channelItemRetriever.getItemsForHourFromLocation(path, location);
                        if (actual.isPresent()) {
                            Links links = actual.get().get_links();
                            return !Arrays.asList(links.getUris()).contains(path) &&
                                    s3Service.confirmItemNotInS3(type, path, channelName);
                        }
                        return false;
                    }
                });
    }
}



