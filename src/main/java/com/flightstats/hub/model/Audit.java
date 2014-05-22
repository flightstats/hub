package com.flightstats.hub.model;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.Builder;

import java.util.Date;

@Builder
@Getter
@ToString
public class Audit {

    private final String user;
    private final String uri;
    private Date date = new Date();

    private static final Gson gson = new GsonBuilder()
            .registerTypeAdapter(Date.class, new HubDateTypeAdapter())
            .create();

    public String toJson() {
        return gson.toJson(this);
    }

    public static Audit fromJson(String json) {
        return gson.fromJson(json, AuditBuilder.class).build();
    }

}
