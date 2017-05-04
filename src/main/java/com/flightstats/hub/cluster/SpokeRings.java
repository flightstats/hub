package com.flightstats.hub.cluster;

import org.joda.time.DateTime;

import java.util.Collection;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

public class SpokeRings implements Ring {


    //todo - gfm - set of rings ordered by start time
    //todo - gfm - we need to protect ourselves from race conditions with this map
    private SortedSet<SpokeRing> spokeRings = new TreeSet<>();

    //todo - gfm - create this by reading events

    /**
     * SpokeRings accepts a list of Strings which follow the format:
     * nodeName + "|" + ctime + "|" + type;
     * This class is responsible for:
     * parsing the values
     * determining if there are any changes
     * if there are changes, create the new rings
     * if any old record exist, clean those up
     */
    public void processChanges(List<String> changes) {


    }


    @Override
    public Collection<SpokeNode> getNodes(String channel) {
        //todo - gfm - return the current set from the latest SpokeRing
        return null;
    }

    @Override
    public Collection<SpokeNode> getNodes(String channel, DateTime pointInTime) {

        return null;
    }

    @Override
    public Collection<SpokeNode> getNodes(String channel, DateTime startTime, DateTime endTime) {

        return null;
    }


}
