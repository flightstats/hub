package com.flightstats.hub.dao;

import com.flightstats.hub.model.NamedType;

import java.util.Collection;

public class ReadOnlyDao<T extends NamedType> implements Dao<T> {
    private final Dao<T> delegate;

    public ReadOnlyDao(Dao<T> delegate) {
        this.delegate = delegate;
    }


    @Override
    public void upsert(T t) {
        throw new UnsupportedOperationException("Unable to write to type due to r/o DAO: " + t);
    }

    @Override
    public T get(String name) {
        return delegate.get(name);
    }

    @Override
    public Collection<T> getAll(boolean useCache) {
        return delegate.getAll(useCache);
    }

    @Override
    public void delete(String name) {
        throw new UnsupportedOperationException("Unable to delete type due to r/o DAO: " + name);
    }

    @Override
    public boolean refresh() {
        return delegate.refresh();
    }
}
