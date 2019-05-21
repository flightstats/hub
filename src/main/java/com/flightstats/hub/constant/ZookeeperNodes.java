package com.flightstats.hub.constant;

public class ZookeeperNodes {

    /**
     * REPLICATED_LAST_UPDATED is set to the last second updated, inclusive of that entire second.
     */
    public static final String REPLICATED_LAST_UPDATED = "/ReplicatedLastUpdated/";
    public static final String REPLICATOR_WATCHER_PATH = "/replicator/watcher";
    public static final String HISTORICAL_EARLIEST = "/HistoricalEarliest/";
    public static final String WEBHOOK_LAST_COMPLETED = "/GroupLastCompleted/";
    public static final String WEBHOOK_LEADER = "/WebhookLeader";
    public static final String LAST_SINGLE_VERIFIED = "/S3VerifierSingleLastVerified/";
    public static final String LAST_COMMITTED_CONTENT_KEY = "/ChannelLatestUpdated/";
}
