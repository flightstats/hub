package com.flightstats.hub.kubernetes;

public enum ServiceName {

    CALLBACK_SERVER("callback-server"),
    DYNAMODB("dynamodb"),
    HUB("hub"),
    HUB_INTERNAL("hub-internal"),
    S3("s3"),
    ZOOKEEPER("zookeeper"),
    ZOOKEEPER_INTERNAL("zk-svc-internal");

    private String value;

    ServiceName (String value){
        this.value = value;
    }

    public String value(){
        return value;
    }
}
