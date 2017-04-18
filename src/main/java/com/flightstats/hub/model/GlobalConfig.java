package com.flightstats.hub.model;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.lang3.StringUtils;

import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;

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
        satellites.forEach(this::addSatellite);
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

    GlobalConfig cleanup() {
        GlobalConfig globalConfig = new GlobalConfig();
        globalConfig.setMaster(master);
        globalConfig.addSatellites(satellites);
        globalConfig.setIsMaster(isMaster);
        return globalConfig;
    }

    public String getMaster() {
        return this.master;
    }

    public Set<String> getSatellites() {
        return this.satellites;
    }

    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof GlobalConfig)) return false;
        final GlobalConfig other = (GlobalConfig) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$master = this.getMaster();
        final Object other$master = other.getMaster();
        if (this$master == null ? other$master != null : !this$master.equals(other$master)) return false;
        final Object this$satellites = this.getSatellites();
        final Object other$satellites = other.getSatellites();
        if (this$satellites == null ? other$satellites != null : !this$satellites.equals(other$satellites))
            return false;
        if (this.isMaster() != other.isMaster()) return false;
        return true;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $master = this.getMaster();
        result = result * PRIME + ($master == null ? 43 : $master.hashCode());
        final Object $satellites = this.getSatellites();
        result = result * PRIME + ($satellites == null ? 43 : $satellites.hashCode());
        result = result * PRIME + (this.isMaster() ? 79 : 97);
        return result;
    }

    protected boolean canEqual(Object other) {
        return other instanceof GlobalConfig;
    }

    public String toString() {
        return "com.flightstats.hub.model.GlobalConfig(master=" + this.getMaster() + ", satellites=" + this.getSatellites() + ", isMaster=" + this.isMaster() + ")";
    }
}
