package com.flightstats.hub.system.resiliency;

import com.flightstats.hub.kubernetes.HubLifecycle;
import com.flightstats.hub.system.extension.SingletonTestInjectionExtension;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.RegisterExtension;

import javax.inject.Inject;

/*
    A temporary example of how SingletonTestInjectionExtension can be used with custom hub setup
*/
@Slf4j
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PlaceholderTest {

    @RegisterExtension
    static SingletonTestInjectionExtension preTest = SingletonTestInjectionExtension.builder()
            .hubDockerImage("flightstats/hub:max-items-system-tests7")
            .build();

    @Inject
    private HubLifecycle hubLifecycle;

    @BeforeAll
    void setup() {
        hubLifecycle.setup();
    }

    @AfterAll
    void cleanup() { }


    @Test
    void test() { }
}
