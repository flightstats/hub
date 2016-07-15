package com.flightstats.hub.dao;

import com.flightstats.hub.model.NamedType;

import java.util.Collection;

public interface Dao<T extends NamedType> {

    void upsert(T t);

    default boolean exists(String name) {
        return getCached(name) != null;
    }

    T get(String name);

    T getCached(String name);

    Collection<T> getAll(boolean useCache);

    void delete(String name);

    default boolean refresh() {
        return false;
    }
}
