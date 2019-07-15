package com.flightstats.hub.system.extension;


import com.flightstats.hub.kubernetes.HubLifecycle;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.inject.Inject;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(SingletonTestInjectionExtension.class)
public class TestSingletonClassWrapper {

    @Inject
    private HubLifecycle hubLifecycle;

    @BeforeAll
    void setup() {
        hubLifecycle.setup();
    }
}
