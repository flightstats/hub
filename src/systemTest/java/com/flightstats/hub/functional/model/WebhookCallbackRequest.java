package com.flightstats.hub.functional.model;

import lombok.Data;

import java.util.List;

@Data
public class WebhookCallbackRequest {
    //webhook name
    private String name;
    private List<String> uris;
    private String type;
}
