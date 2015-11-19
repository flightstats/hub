package com.flightstats.hub.metrics;

import com.flightstats.hub.model.Traces;

import java.util.Comparator;
import java.util.TreeSet;

/**
 * This is designed assuming that:
 * 1 - It is a single bottleneck for all transactions
 * 2 - The vast majority of transactions (99.999% per day for a list of 100)
 * will not be in the set
 */
public class TracesSortedSet extends TreeSet<Traces> {

    private final int maxSize;
    private long smallestTime;

    public TracesSortedSet(int maxSize) {
        super(new DescendingTracesComparator());
        this.maxSize = maxSize;
    }

    @Override
    public boolean add(Traces traces) {
        if (size() >= maxSize) {
            boolean result = false;
            if (traces.getTime() > smallestTime) {
                synchronized (this) {
                    Traces smallest = last();
                    remove(smallest);
                    result = super.add(traces);
                    smallestTime = smallest.getTime();
                }
            }
            return result;
        } else {
            synchronized (this) {
                if (isEmpty()) {
                    smallestTime = traces.getTime();
                } else {
                    smallestTime = Math.min(last().getTime(), traces.getTime());
                }
                return super.add(traces);
            }
        }
    }

    static class DescendingTracesComparator implements Comparator<Traces> {
        @Override
        public int compare(Traces t1, Traces t2) {
            int difference = (int) (t2.getTime() - t1.getTime());
            if (difference == 0) {
                difference = t1.getId().compareTo(t2.getId());
            }
            return difference;
        }
    }
}
