package com.flightstats.hub.constant;

public class InternalResourceDescription {

    public static final String CHANNEL_DESCRIPTION = "Delete, refresh, and check the staleness of channels.";
    public static final String CLUSTER_DESCRIPTION = "Information about the cluster and decommissioning nodes.";
    public static final String DEPLOY_DESCRIPTION = "Get a list of hubs to deploy to in a cluster.";
    public static final String HEALTH_DESCRIPTION = "See status of all hubs in a cluster.";
    public static final String PROPERTIES_DESCRIPTION = "Get hub properties with links to other hubs in the cluster.";
    public static final String SHUTDOWN_DESCRIPTION = "See if any server is being shutdown, shutdown a node, and reset the shutdown lock.";
    public static final String S3MM_DESCRIPTION = "Introspect into S3 lifecycle rule maintenance.";
    public static final String STACKTRACE_DESCRIPTION = "Get a condensed stacktrace with links to other hubs in the cluster.";
    public static final String TIME_DESCRIPTION = "Links for managing time in a hub cluster.";
    public static final String TRACES_DESCRIPTION = "Shows active requests, the slowest 100, and the latest 100 with links to other hubs in the cluster";
    public static final String WEBHOOK_DESCRIPTION = "Get all webhooks, or stale or erroring webhooks.";
    public static final String ZOOKEEPER_DESCRIPTION = "Read-only interface into the ZooKeeper hierarchy.";
}
