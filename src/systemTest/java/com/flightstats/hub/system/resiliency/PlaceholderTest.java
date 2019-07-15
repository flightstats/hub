package com.flightstats.hub.system.resiliency;

import com.flightstats.hub.system.extension.TestSingletonClassWrapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

/*
     A temporary example of how TestSingletonClassWrapper can be used with custom hub setup
*/
@Slf4j
public class PlaceholderTest extends TestSingletonClassWrapper {
    public static final String IMAGE = "flightstats/hub:max-items-system-tests7";

    @AfterAll
    void cleanup() { }

    @Test
    void test() { }
}
