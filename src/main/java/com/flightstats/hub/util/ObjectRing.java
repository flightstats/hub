package com.flightstats.hub.util;

import java.util.ArrayList;
import java.util.List;

public class ObjectRing<T> {

    private T[] items;
    private int position = 0;
    private int totalSize = 0;

    public ObjectRing(int size) {
        items = (T[]) new Object[size];
    }

    public synchronized void put(T item) {
        if (position == items.length) {
            position = 0;
        }
        items[position] = item;
        position++;
        totalSize++;
    }

    public List<T> getItems() {
        List<T> list = new ArrayList<>();
        int start = position;
        for (int i = start; i < items.length; i++) {
            if (null != items[i]) {
                list.add(items[i]);
            }
        }
        for (int i = 0; i < start; i++) {
            if (null != items[i]) {
                list.add(items[i]);
            }
        }
        return list;
    }

    public int getTotalSize() {
        return totalSize;
    }
}
