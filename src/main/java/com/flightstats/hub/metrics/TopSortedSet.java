package com.flightstats.hub.metrics;

import java.util.Comparator;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Function;

/**
 * This is designed assuming that:
 * 1 - It is a single bottleneck for all transactions
 * 2 - The vast majority of transactions (99.999% per day for a list of 100)
 * will not be in the set
 */
public class TopSortedSet<E> extends TreeSet<E> {

    private final int maxSize;
    private final Comparator comparator;
    private final Function<E, Long> metricFunction;
    private long smallestTime;

    public TopSortedSet(int maxSize, Function<E, Long> metricFunction, Comparator<E> comparator) {
        super(comparator);
        this.maxSize = maxSize;
        this.comparator = comparator;
        this.metricFunction = metricFunction;
    }

    @Override
    public boolean add(E e) {
        Long apply = metricFunction.apply(e);
        if (size() >= maxSize) {
            boolean result = false;

            if (apply > smallestTime) {
                synchronized (this) {
                    E smallest = last();
                    remove(smallest);
                    result = super.add(e);
                    smallestTime = metricFunction.apply(smallest);
                    while (size() > maxSize) {
                        remove(last());
                    }
                }
            }
            return result;
        } else {
            synchronized (this) {
                if (isEmpty()) {
                    smallestTime = apply;
                } else {
                    smallestTime = Math.min(metricFunction.apply(last()), apply);
                }
                return super.add(e);
            }
        }
    }

    public SortedSet<E> getCopy() {
        TreeSet<E> copy = new TreeSet<>(comparator);
        synchronized (this) {
            copy.addAll(this);
        }
        return copy;
    }

}
