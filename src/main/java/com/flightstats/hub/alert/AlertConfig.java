package com.flightstats.hub.alert;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flightstats.hub.app.HubProperties;
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

    //todo - gfm - 6/10/15 - rename to source
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

    public static AlertConfig fromJson(String name, String json) {
        AlertConfig alertConfig = gson.fromJson(json, AlertConfig.class);
        alertConfig.hubDomain = HubProperties.getAppUrl();
        alertConfig.name = name;
        if (alertConfig.type == null) {
            alertConfig.type = AlertType.channel;
        }
        return alertConfig;
    }

    @JsonIgnore
    public boolean isChannelAlert() {
        return type == AlertType.channel;
    }

    @JsonIgnore
    public String getHubDomain() {
        return hubDomain;
    }

    public String getAlertDescription(int count) {
        if (isChannelAlert()) {
            return getName() + ": " + getHubDomain() + "channel/" + getChannel() + " volume " +
                    count + " " + getOperator() + " " + getThreshold();
        } else {
            return getName() + ": " + getHubDomain() + "group/" + getChannel() + " is " + count + " minutes behind";
        }
    }

    public void writeJson(ObjectNode node) {
        ObjectNode alertConfig = node.putObject(name);
        alertConfig.put("channel", channel);
        alertConfig.put("serviceName", serviceName);
        alertConfig.put("timeWindowMinutes", timeWindowMinutes);
        if (isChannelAlert()) {
            alertConfig.put("type", "channel");
            alertConfig.put("operator", operator);
            alertConfig.put("threshold", threshold);
        } else {
            alertConfig.put("type", "group");
        }
    }
}
