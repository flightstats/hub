package com.flightstats.hub.system.extension;

import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GuiceProviderExtension.class)
@ExtendWith(DependencyInjectionExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TestSuiteClassWrapper {
}
