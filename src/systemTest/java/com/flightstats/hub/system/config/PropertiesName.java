package com.flightstats.hub.system.config;

public class PropertiesName {
    public static final String HELM_RELEASE_NAME = "helm.release.name";
    static final String HELM_CHART_PATH = "helm.chart.path";
    static final String HELM_CLUSTERED_HUB = "helm.release.hub.isClustered";
    static final String HELM_RELEASE_DELETE = "helm.release.delete";

    public static final String HUB_URL_TEMPLATE = "hub.url";
    static final String CALLBACK_URL_TEMPLATE = "callback.url";
    public static final String HUB_DOCKER_IMAGE = "hub.docker.image";

    public static final String AWS_REGION = "aws.region";
    public static final String S3_CREDENTIALS_PATH = "aws.credentials.path";

    public static final String S3_URL_TEMPLATE = "s3.url";
    public static final String S3_BUCKET_TEMPLATE = "s3.bucket.name";

    public static final String DYNAMODB_URL_TEMPLATE = "dynamodb.url";
    public static final String DYNAMODB_CHANNEL_CONFIG_TABLE = "dynamodb.tables.channel_config";
}
