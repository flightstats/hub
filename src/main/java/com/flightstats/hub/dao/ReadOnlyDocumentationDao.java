package com.flightstats.hub.dao;

public class ReadOnlyDocumentationDao implements DocumentationDao {
    private final DocumentationDao delegate;

    public ReadOnlyDocumentationDao(DocumentationDao delegate) {
        this.delegate = delegate;
    }

    @Override
    public String get(String channel) {
        return delegate.get(channel);
    }

    @Override
    public boolean upsert(String channel, byte[] content) {
        throw new UnsupportedOperationException("Unable to upsert channel documentation due to r/o DAO " + channel);
    }

    @Override
    public boolean delete(String channel) {
        throw new UnsupportedOperationException("Unable to delete channel documentation due to r/o DAO " + channel);
    }
}
