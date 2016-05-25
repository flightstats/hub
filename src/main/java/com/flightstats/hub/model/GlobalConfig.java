package com.flightstats.hub.model;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;

import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;

@ToString
@EqualsAndHashCode()
@Getter
public class GlobalConfig {

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
                global.addSatellite(satNode.asText());
            }
        }
        return global;
    }

    public void addSatellite(String satellite) {
        satellites.add(StringUtils.appendIfMissing(satellite, "/"));
    }

    public void addSatellites(Collection<String> satellites) {
        for (String satellite : satellites) {
            addSatellite(satellite);
        }
    }

    public void setIsMaster(boolean isMaster) {
        this.isMaster = isMaster;
    }

    public boolean isMaster() {
        return isMaster;
    }

    public void setMaster(String master) {
        this.master = StringUtils.appendIfMissing(master, "/");
    }
}
