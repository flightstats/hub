package com.flightstats.hub.util;

public class ObjectRing<T> {

    private T[] items;
    private int position = 0;

    public ObjectRing(int size) {
        items = (T[]) new Object[size];
    }

    public synchronized void put(T item) {
        if (position == items.length) {
            position = 0;
        }
        items[position] = item;
        position++;
    }

    public T[] getItems() {
        return items;
    }
}
