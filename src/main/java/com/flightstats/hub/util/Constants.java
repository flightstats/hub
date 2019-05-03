package com.flightstats.hub.util;

public class Constants {

    /**
     * REPLICATED_LAST_UPDATED is set to the last second updated, inclusive of that entire second.
     */
    public static final String REPLICATOR_WATCHER_PATH = "/replicator/watcher";
    public static final String REPLICATED_LAST_UPDATED = "/ReplicatedLastUpdated/";
    public static final String HISTORICAL_EARLIEST = "/HistoricalEarliest/";
    public static final String CREATION_DATE = "Creation-Date";
    public static final String WEBHOOK_LAST_COMPLETED = "/GroupLastCompleted/";

    public static final String CHANNEL_DESCRIPTION = "Delete, refresh, and check the staleness of channels.";
    public static final String CLUSTER_DESCRIPTION = "Information about the cluster and decommissioning nodes.";
    public static final String DEPLOY_DESCRIPTION = "Get a list of hubs to deploy to in a cluster.";
    public static final String HEALTH_DESCRIPTION = "See status of all hubs in a cluster.";
    public static final String PROPERTIES_DESCRIPTION = "Get hub properties with links to other hubs in the cluster.";
    public static final String SHUTDOWN_DESCRIPTION = "See if any server is being shutdown, shutdown a node, and reset the shutdown lock.";
    public static final String STACKTRACE_DESCRIPTION = "Get a condensed stacktrace with links to other hubs in the cluster.";
    public static final String TIME_DESCRIPTION = "Links for managing time in a hub cluster.";
    public static final String TRACES_DESCRIPTION = "Shows active requests, the slowest 100, and the latest 100 with links to other hubs in the cluster";
    public static final String WEBHOOK_DESCRIPTION = "Get all webhooks, or stale or erroring webhooks.";
    public static final String ZOOKEEPER_DESCRIPTION = "Read-only interface into the ZooKeeper hierarchy.";

    public static final String S3_VERIFIER_CHANNEL_THREAD_POOL = "S3VerifierChannelThreadPool";
    public static final String S3_VERIFIER_QUERY_THREAD_POOL = "S3VerifierQueryThreadPool";
}
