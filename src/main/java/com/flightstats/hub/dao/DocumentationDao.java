package com.flightstats.hub.dao;

public interface DocumentationDao {

    String get(String channel);

    boolean upsert(String channel, byte[] content);

    boolean delete(String channel);
}
