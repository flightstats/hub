package com.flightstats.hub.group;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.ArrayList;
import java.util.List;

public class GroupResponse {
    private final List<String> uris = new ArrayList<>();

    public void add(String uri) {
        uris.add(uri);
    }

    private static final Gson gson = new GsonBuilder().create();

    public String toJson() {
        return gson.toJson(this);
    }

}
