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

    private String name;
    @Deprecated
    private String channel;
    private String source;
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

    public String getSource() {
        if (source == null) {
            return channel;
        }
        return source;
    }

    public String getAlertDescription(int count) {
        if (isChannelAlert()) {
            return getName() + ": " + getHubDomain() + "channel/" + getSource() + " volume " +
                    count + " " + getOperator() + " " + getThreshold();
        } else {
            return getName() + ": " + getHubDomain() + "group/" + getSource() + " is " + count + " minutes behind";
        }
    }

    public void writeJson(ObjectNode node) {
        node.put("name", name);
        node.put("channel", getSource());
        node.put("source", getSource());
        node.put("serviceName", serviceName);
        node.put("timeWindowMinutes", timeWindowMinutes);
        if (isChannelAlert()) {
            node.put("type", "channel");
            node.put("operator", operator);
            node.put("threshold", threshold);
        } else {
            node.put("type", "group");
        }
    }
}
