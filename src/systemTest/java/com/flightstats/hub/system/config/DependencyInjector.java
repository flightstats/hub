package com.flightstats.hub.system.config;

import com.google.inject.Guice;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;

@Slf4j
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class DependencyInjector {

    @BeforeAll
    public void inject() {
        Guice.createInjector(new GuiceModule()).injectMembers(this);
    }

}
