package com.flightstats.hub.model;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.Set;
import java.util.TreeSet;

@ToString
@EqualsAndHashCode()
@Getter
public class GlobalConfig {

    @Setter
    private String master;
    private Set<String> satellites = new TreeSet<>();

    private boolean isMaster = false;

    public static GlobalConfig parseJson(JsonNode globalNode) {
        GlobalConfig global = new GlobalConfig();
        if (globalNode.has("master")) {
            global.setMaster(globalNode.get("master").asText());
        }
        if (globalNode.has("satellites")) {
            global.getSatellites().clear();
            JsonNode satellites = globalNode.get("satellites");
            for (JsonNode satNode : satellites) {
                global.getSatellites().add(satNode.asText());
            }
        }
        return global;
    }

    public void setIsMaster(boolean isMaster) {
        this.isMaster = isMaster;
    }

    public boolean isMaster() {
        return isMaster;
    }
}
