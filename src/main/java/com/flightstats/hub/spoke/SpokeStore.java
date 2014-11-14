package com.flightstats.hub.spoke;

public interface SpokeStore {

    public static final String FILE = "FileSpokeStore";
    public static final String REMOTE = "RemoteSpokeStore";

    boolean write(String path, byte[] payload) throws Exception;

    byte[] read(String path) throws Exception;

    boolean delete(String path) throws Exception;
}
