package com.flightstats.hub.metrics;

import java.util.Comparator;

class DescendingTracesComparator implements Comparator<Traces> {
    @Override
    public int compare(Traces t1, Traces t2) {
        int difference = (int) (t2.getTime() - t1.getTime());
        if (difference == 0) {
            difference = t1.getId().compareTo(t2.getId());
        }
        return difference;
    }
}
