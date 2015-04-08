package com.flightstats.hub.alert;

import com.google.gson.Gson;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Builder
@Getter
@ToString
public class AlertConfig {

    private static final Gson gson = new Gson();
    private String name;

    private String hubDomain;
    private String channel;
    private String serviceName;
    private String operator;
    private int threshold;
    private int minutes;

    public static AlertConfig fromJson(String name, String hubDomain, String json) {
        AlertConfig alertConfig = gson.fromJson(json, AlertConfig.class);
        alertConfig.hubDomain = hubDomain;
        alertConfig.name = name;
        return alertConfig;
    }
}
