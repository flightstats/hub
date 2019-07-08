package com.flightstats.hub.system.resiliency;

import com.flightstats.hub.kubernetes.HubLifecycle;
import com.flightstats.hub.system.extension.SingletonTestInjectionExtension;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.inject.Inject;

/*
    A temporary example of how SingletonTestInjectionExtension can be used with custom hub setup
*/
@Slf4j
@ExtendWith(SingletonTestInjectionExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PlaceholderTest {
    private static final String CUSTOM_YAML_FOR_HELM = "hub: \n" +
            "  hub: \n" +
            "    image: flightstats/hub:latest \n" +
            "    configMap: \n" +
            "      properties: \n" +
            "        can.set.custom.properties: here";
    @Inject
    private HubLifecycle hubLifecycle;

    @BeforeAll
    void setup() {
        hubLifecycle.setup(CUSTOM_YAML_FOR_HELM);
    }

    @AfterAll
    void cleanup() { }


    @Test
    void test() { }
}
