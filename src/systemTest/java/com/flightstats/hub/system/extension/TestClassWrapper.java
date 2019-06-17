package com.flightstats.hub.system.extension;


import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;

@Slf4j
@ExtendWith(GuiceProviderExtension.class)
@ExtendWith(HubLifecycleSuiteExtension.class)
@ExtendWith(DependencyInjectionExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TestClassWrapper {
}
