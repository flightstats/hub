package com.flightstats.hub.dao.aws;

import com.amazonaws.services.s3.model.BucketLifecycleConfiguration;
import com.flightstats.hub.model.ChannelConfig;
import com.flightstats.hub.util.StringUtils;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class S3ConfigStrategyTest {

    private TreeSet<String> allRules = new TreeSet<>();
    private final List<ChannelConfig> channelConfigs = new ArrayList<>();

    @Test
    void testThree() {
        verify(240, 3);
    }

    @Test
    void testTwo() {
        verify(160, 2);
    }

    @Test
    void testOne() {
        verify(100, 1);
    }

    private void verify(int channels, int iterations) {
        List<String> names = new ArrayList<>();
        for (int i = 0; i < channels; i++) {
            String name = StringUtils.randomAlphaNumeric(10);
            channelConfigs.add(ChannelConfig.builder().name(name).build());
            String batchedName = name + "Batch";
            names.add(batchedName);
        }
        for (int i = 0; i < iterations; i++) {
            addRuleNames(new DateTime(2016, 1, 2 * i + 1, 1, 1));
        }
        assertEquals(names.size(), allRules.size());
        assertTrue(allRules.containsAll(names));
    }

    private void addRuleNames(DateTime timeForSharding) {
        int max = 100;
        List<BucketLifecycleConfiguration.Rule> rules = S3ConfigStrategy.apportion(channelConfigs, timeForSharding, max);
        assertEquals(max, rules.size());
        allRules.addAll(rules.stream().map(BucketLifecycleConfiguration.Rule::getId).collect(Collectors.toList()));
    }

}