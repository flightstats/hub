package com.flightstats.hub.alert;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

@Builder
@Getter
@ToString
@EqualsAndHashCode
public class AlertStatus {

    private static final Gson gson = new GsonBuilder().disableHtmlEscaping().create();

    private String name;
    private List<AlertStatusHistory> history;
    private boolean alert;

    public static Map<String, AlertStatus> fromJson(String json) {
        Type mapType = new TypeToken<Map<String, AlertStatus>>() {
        }.getType();
        Map<String, AlertStatus> map = gson.fromJson(json, mapType);

        return map;
    }

    public static String toJson(Map<String, AlertStatus> map) {
        return gson.toJson(map);
    }
}
