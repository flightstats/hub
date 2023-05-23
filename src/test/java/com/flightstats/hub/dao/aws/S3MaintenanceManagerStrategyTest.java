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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class S3MaintenanceManagerStrategyTest {

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
            ChannelConfig config = ChannelConfig.builder().name(name).build();
            channelConfigs.add(config);
            names.add(S3ConfigStrategy.getChannelTypedName(config, S3ConfigStrategy.BATCH_POSTFIX));
        }
        for (int i = 0; i < iterations; i++) {
            addRuleNames(new DateTime(2016, 1, 2 * i + 1, 1, 1));
        }

        assertEquals(names.size(), allRules.size());
        names.forEach(name->{
            String nameWithPrefix = S3ConfigStrategy.BUCKET_LIFECYCLE_RULE_PREFIX.concat(name);
            assertTrue(allRules.contains(nameWithPrefix));
        });
    }

    private void addRuleNames(DateTime timeForSharding) {
        int max = 100;
        List<BucketLifecycleConfiguration.Rule> rules = S3ConfigStrategy.apportion(channelConfigs, timeForSharding, max);
        assertEquals(max, rules.size());
        allRules.addAll(rules.stream().map(BucketLifecycleConfiguration.Rule::getId).collect(Collectors.toList()));
    }

    @Test
    public void testGetNonHubBucketLifecycleRules(){
        List<BucketLifecycleConfiguration.Rule> rules = new ArrayList<>();
        rules.add(new BucketLifecycleConfiguration.Rule().withId(S3ConfigStrategy.BUCKET_LIFECYCLE_RULE_PREFIX + "channel1_rule"));
        rules.add(new BucketLifecycleConfiguration.Rule().withId(S3ConfigStrategy.BUCKET_LIFECYCLE_RULE_PREFIX + "channel2_rule"));
        rules.add(new BucketLifecycleConfiguration.Rule().withId("terraform_rule1"));
        rules.add(new BucketLifecycleConfiguration.Rule().withId("terraform_rule2"));

        List<BucketLifecycleConfiguration.Rule> filteredRules =
                S3ConfigStrategy.getNonHubBucketLifecycleRules(new BucketLifecycleConfiguration().withRules(rules));
        assertTrue(filteredRules.size() == 2);
        assertFalse(filteredRules.get(0).getId().startsWith(S3ConfigStrategy.BUCKET_LIFECYCLE_RULE_PREFIX));
        assertFalse(filteredRules.get(1).getId().startsWith(S3ConfigStrategy.BUCKET_LIFECYCLE_RULE_PREFIX));

    }
}