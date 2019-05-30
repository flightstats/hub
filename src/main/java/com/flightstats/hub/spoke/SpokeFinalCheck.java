package com.flightstats.hub.spoke;

import com.flightstats.hub.app.FinalCheck;
import javax.inject.Inject;

public class SpokeFinalCheck implements FinalCheck {

    @Inject
    private SpokeClusterHealthCheck healthCheck;

    @Override
    public boolean check() throws Exception {
        return healthCheck.testAll();
    }

}