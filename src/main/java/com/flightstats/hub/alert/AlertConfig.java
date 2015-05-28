package com.flightstats.hub.alert;

import com.google.gson.Gson;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Builder
@Getter
@ToString
@EqualsAndHashCode
public class AlertConfig {

    private static final Gson gson = new Gson();

    private transient String hubDomain;

    private String name;
    private String channel;
    private String serviceName;
    private String operator;
    private int threshold;
    private int timeWindowMinutes;
    private AlertType alertType;

    enum AlertType {
        CHANNEL,
        GROUP
    }

    public static AlertConfig fromJson(String name, String hubDomain, String json, AlertType alertType) {
        AlertConfig alertConfig = gson.fromJson(json, AlertConfig.class);
        alertConfig.hubDomain = hubDomain;
        alertConfig.name = name;
        alertConfig.alertType = alertType;
        return alertConfig;
    }

    public boolean isChannelAlert() {
        return alertType == AlertType.CHANNEL;
    }

    public String getAlertDescription(int count) {
        if (isChannelAlert()) {
            return getName() + ": " + getHubDomain() + "channel/" + getChannel() + " volume " +
                    count + " " + getOperator() + " " + getThreshold();
        } else {
            return getName() + ": " + getHubDomain() + "group/" + getChannel() + " is " + count + " minutes behind";
        }
    }
}
