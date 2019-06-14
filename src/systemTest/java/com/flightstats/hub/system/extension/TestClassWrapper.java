package com.flightstats.hub.system.extension;


import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.extension.RegisterExtension;

@Slf4j
public class TestClassWrapper {
    @RegisterExtension
    @Order(0)
    static GuiceProviderExtension guiceProviderExtension = new GuiceProviderExtension();

    @RegisterExtension
    @Order(1)
    static DependencyInjectionResolver dependencyInjectionResolver = new DependencyInjectionResolver();

    @RegisterExtension
    @Order(2)
    static HubLifecycleSuiteExtension hubLifecycleSuiteExtension = new HubLifecycleSuiteExtension();

    @RegisterExtension
    @Order(3)
    DependencyInjectionExtension dependencyInjectionExtension = new DependencyInjectionExtension();
}
