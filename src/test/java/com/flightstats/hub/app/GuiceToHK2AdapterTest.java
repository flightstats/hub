package com.flightstats.hub.app;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.google.inject.name.Named;
import lombok.Value;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.api.ServiceLocatorFactory;
import org.glassfish.hk2.utilities.ServiceLocatorUtilities;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class GuiceToHK2AdapterTest {

    @Value
    private static class SimpleService {
        String name;

        SimpleService() {
            this("unnamed");
        }

        SimpleService(String name) {
            this.name = name;
        }

    }

    private static class NativeService {
        @javax.inject.Inject
        NativeService() {}
    }

    private static class GuicedService {
        @com.google.inject.Inject
        GuicedService() {}
    }

    @javax.inject.Singleton
    private static class NativeSingletonService {}

    @com.google.inject.Singleton
    private static class GuicedSingletonService {}

    @Test
    public void testSimple() {
        ServiceLocator locator = initializeHK2(buildEmptyInjector());
        locator.getService(NativeService.class);
        locator.getService(GuicedService.class);
    }

    @Test
    public void testProvides() {
        Injector injector = Guice.createInjector(new AbstractModule() {
            @Override
            protected void configure() {}

            @Provides
            SimpleService providesService() {
                return new SimpleService();
            }
        });
        ServiceLocator locator = initializeHK2(injector);
        locator.getService(SimpleService.class);
    }

    @Test
    public void testProvidesNamed() {
        Injector injector = Guice.createInjector(new AbstractModule() {
            @Override
            protected void configure() {}

            @Provides
            @Named("foo")
            SimpleService providesFooService() {
                return new SimpleService("foo");
            }

            @Provides
            @Named("bar")
            SimpleService providesBarService() {
                return new SimpleService("bar");
            }
        });
        ServiceLocator locator = initializeHK2(injector);
        SimpleService service = locator.getService(SimpleService.class, "foo");
        assertEquals("foo", service.getName());
    }

    @Test
    public void testNativeEagerSingleton() {
        Injector injector = Guice.createInjector(new AbstractModule() {
            @Override
            protected void configure() {
                bind(NativeService.class).asEagerSingleton();
            }
        });
        ServiceLocator locator = initializeHK2(injector);
        NativeService a = locator.getService(NativeService.class);
        NativeService b = locator.getService(NativeService.class);
        assertEquals(a, b);

    }

    @Test
    public void testGuicedEagerSingleton() {
        Injector injector = Guice.createInjector(new AbstractModule() {
            @Override
            protected void configure() {
                bind(GuicedService.class).asEagerSingleton();
            }
        });
        ServiceLocator locator = initializeHK2(injector);
        GuicedService a = locator.getService(GuicedService.class);
        GuicedService b = locator.getService(GuicedService.class);
        assertEquals(a, b);

    }

    @Test
    public void testNativeAnnotatedSingleton() {
        ServiceLocator locator = initializeHK2(buildEmptyInjector());
        NativeSingletonService a = locator.getService(NativeSingletonService.class);
        NativeSingletonService b = locator.getService(NativeSingletonService.class);
        assertEquals(a, b);
    }

    @Test
    public void testGuicedAnnotatedSingleton() {
        ServiceLocator locator = initializeHK2(buildEmptyInjector());
        GuicedSingletonService a = locator.getService(GuicedSingletonService.class);
        GuicedSingletonService b = locator.getService(GuicedSingletonService.class);
        assertEquals(a, b);
    }

    private Injector buildEmptyInjector() {
        return Guice.createInjector(new AbstractModule() {
            @Override
            protected void configure() {}
        });
    }

    private ServiceLocator initializeHK2(Injector injector) {
        GuiceToHK2Adapter adapter = new GuiceToHK2Adapter(injector);
        ServiceLocatorFactory factory = ServiceLocatorFactory.getInstance();
        ServiceLocator locator = factory.create(null);
        ServiceLocatorUtilities.bind(locator, adapter);
        return locator;
    }

}
