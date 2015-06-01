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
    private AlertType type;

    enum AlertType {
        channel,
        group
    }

    public static AlertConfig fromJson(String name, String hubDomain, String json) {
        AlertConfig alertConfig = gson.fromJson(json, AlertConfig.class);
        alertConfig.hubDomain = hubDomain;
        alertConfig.name = name;
        if (alertConfig.type == null) {
            alertConfig.type = AlertType.channel;
        }
        return alertConfig;
    }

    public boolean isChannelAlert() {
        return type == AlertType.channel;
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
