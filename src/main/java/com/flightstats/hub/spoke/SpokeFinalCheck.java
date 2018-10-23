package com.flightstats.hub.spoke;

import com.flightstats.hub.app.FinalCheck;

import javax.inject.Inject;

public class SpokeFinalCheck implements FinalCheck {

    private final RemoteSpokeStore remoteSpokeStore;

    @Inject
    SpokeFinalCheck(RemoteSpokeStore remoteSpokeStore) {
        this.remoteSpokeStore = remoteSpokeStore;
    }

    @Override
    public boolean check() throws Exception {
        return remoteSpokeStore.testAll();
    }
}
