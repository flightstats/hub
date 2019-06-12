package com.flightstats.hub.model;

import lombok.Data;

import java.util.List;

@Data
public class WebhookCallback {
    //webhook name
    private String name;
    private List<String> uris;
    private String type;

}
