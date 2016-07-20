package com.flightstats.hub.dao.aws;

import com.amazonaws.services.s3.model.BucketLifecycleConfiguration;
import com.flightstats.hub.model.ChannelConfig;
import org.apache.commons.codec.digest.DigestUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class S3ConfigStrategy {

    private final static Logger logger = LoggerFactory.getLogger(S3ConfigStrategy.class);

    static List<BucketLifecycleConfiguration.Rule> apportion(Iterable<ChannelConfig> channelConfigs, DateTime timeForSharding, int max) {
        List<BucketLifecycleConfiguration.Rule> rules = new ArrayList<>();
        for (ChannelConfig config : channelConfigs) {
            addRule(rules, config);
        }
        if (rules.size() <= max) {
            return rules;
        }
        return handleMax(channelConfigs, timeForSharding, max, rules.size());
    }

    private static List<BucketLifecycleConfiguration.Rule> handleMax(Iterable<ChannelConfig> channelConfigs, DateTime timeForSharding, int max, double rulesCount) {
        int buckets = (int) Math.ceil(rulesCount / (0.8 * max));
        Map<Integer, List<BucketLifecycleConfiguration.Rule>> shardedRules = new HashMap<>();
        for (ChannelConfig config : channelConfigs) {
            byte[] md5 = DigestUtils.md5(config.getName());
            int mod = Math.abs(md5[0]) % buckets;
            List<BucketLifecycleConfiguration.Rule> ruleList = shardedRules.getOrDefault(mod, new ArrayList<>());
            shardedRules.put(mod, ruleList);
            addRule(ruleList, config);

        }
        int days = 2;
        int activeShard = timeForSharding.getDayOfYear() / days % buckets;
        logger.info("getDayOfYear {} buckets {} activeShard {}", timeForSharding.getDayOfYear(), buckets, activeShard);
        List<BucketLifecycleConfiguration.Rule> rules = shardedRules.get(activeShard);
        logger.info("base rules  {}", rules.size());
        if (rules.size() < max) {
            activeShard++;
            if (activeShard == buckets) {
                activeShard = 0;
            }
            List<BucketLifecycleConfiguration.Rule> nextRules = shardedRules.get(activeShard);
            int additionalRules = max - rules.size();
            for (int i = 0; i < additionalRules; i++) {
                rules.add(nextRules.get(i));
            }
        }
        logger.info("total rules {}", rules.size());
        logger.info("shardedRules {} keys {}", shardedRules.size(), shardedRules.keySet());
        return rules;
    }

    private static void addRule(List<BucketLifecycleConfiguration.Rule> rules, ChannelConfig config) {
        if (config.getTtlDays() > 0) {
            if (config.isSingle() || config.isBoth()) {
                rules.add(createRule(config, ""));
            }
            if (config.isBatch() || config.isBoth()) {
                rules.add(createRule(config, "Batch"));
            }
        }
    }

    private static BucketLifecycleConfiguration.Rule createRule(ChannelConfig config, String postfix) {
        String id = config.getName() + postfix;
        return new BucketLifecycleConfiguration.Rule()
                .withPrefix(id + "/")
                .withId(id)
                .withExpirationInDays((int) config.getTtlDays())
                .withStatus(BucketLifecycleConfiguration.ENABLED);
    }
}
