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
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Collectors;

import static com.google.common.collect.Lists.newArrayList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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

    @Value
    private static class NativeService {
        SimpleService simpleService;

        @javax.inject.Inject
        NativeService(SimpleService simpleService) {
            this.simpleService = simpleService;
        }
    }

    @Value
    private static class GuicedService {
        SimpleService simpleService;

        @com.google.inject.Inject
        GuicedService(SimpleService simpleService) {
            this.simpleService = simpleService;
        }
    }

    @javax.inject.Singleton
    private static class NativeSingletonService {}

    @com.google.inject.Singleton
    private static class GuicedSingletonService {}


    private static class ComplicatedSimpleService<T extends SimpleService> {
        List<T> list;
        @com.google.inject.Inject
        public ComplicatedSimpleService(List<T> whateverList) {
            this.list = whateverList;
        }

        List<String> getNames() {
            return list.stream().map(SimpleService::getName).collect(Collectors.toList());
        }
    }

    private static class WithComplicatedDependencyService {
        final ComplicatedSimpleService<SimpleService> dependency;

        @javax.inject.Inject
        public WithComplicatedDependencyService(ComplicatedSimpleService<SimpleService> dependency) {
            this.dependency = dependency;
        }
    }

    @Test
    public void testSimple() {
        Injector injector = Guice.createInjector(new AbstractModule() {
            @Override
            protected void configure() {
                bind(NativeService.class);
                bind(GuicedService.class);
            }
        });
        ServiceLocator locator = initializeHK2(injector);
        NativeService nativeService = locator.getService(NativeService.class);
        GuicedService guicedService = locator.getService(GuicedService.class);
        assertNotNull(nativeService);
        assertNotNull(nativeService.getSimpleService());
        assertNotNull(guicedService);
        assertNotNull(guicedService.getSimpleService());
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
        SimpleService service = locator.getService(SimpleService.class);
        assertNotNull(service);
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
        assertNotNull(service);
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
        assertNotNull(a);
        assertNotNull(b);
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
        assertNotNull(a);
        assertNotNull(b);
        assertEquals(a, b);

    }

    @Test
    public void testNativeAnnotatedSingleton() {
        Injector injector = Guice.createInjector(new AbstractModule() {
            @Override
            protected void configure() {
                bind(NativeSingletonService.class).asEagerSingleton();
            }
        });
        ServiceLocator locator = initializeHK2(injector);
        NativeSingletonService a = locator.getService(NativeSingletonService.class);
        NativeSingletonService b = locator.getService(NativeSingletonService.class);
        assertNotNull(a);
        assertNotNull(b);
        assertEquals(a, b);
    }

    @Test
    public void testGuicedAnnotatedSingleton() {
        Injector injector = Guice.createInjector(new AbstractModule() {
            @Override
            protected void configure() {
                bind(GuicedSingletonService.class).asEagerSingleton();
            }
        });
        ServiceLocator locator = initializeHK2(injector);
        GuicedSingletonService a = locator.getService(GuicedSingletonService.class);
        GuicedSingletonService b = locator.getService(GuicedSingletonService.class);
        assertNotNull(a);
        assertNotNull(b);
        assertEquals(a, b);
    }

    @Test
    public void testComplicatedDependencyWithGenericAndProvider() {
        Injector injector = Guice.createInjector(new AbstractModule() {
            @Override
            protected void configure() {
                bind(WithComplicatedDependencyService.class).asEagerSingleton();
            }

            @Provides
            public ComplicatedSimpleService<SimpleService> buildSomeServices() {
                return new ComplicatedSimpleService<>(newArrayList(new SimpleService("just one")));
            }
        });

        ServiceLocator locator = initializeHK2(injector);
        WithComplicatedDependencyService service = locator.getService(WithComplicatedDependencyService.class);
        assertNotNull(service);
        assertEquals(newArrayList("just one"), service.dependency.getNames());
    }

    @Test
    public void testNamedComplicatedDependencyWithGenericAndProvider() {
        Injector injector = Guice.createInjector(new AbstractModule() {
            @Override
            protected void configure() { }

            @Provides
            public WithComplicatedDependencyService buildComplicatedService(@Named("aDependency") ComplicatedSimpleService<SimpleService> aDependency) {
                return new WithComplicatedDependencyService(aDependency);
            }

            @Named("aDependency")
            @Provides
            public ComplicatedSimpleService<SimpleService> buildAService() {
                return new ComplicatedSimpleService<>(newArrayList(new SimpleService("just one")));
            }

            @Named("anotherDependency")
            @Provides
            public ComplicatedSimpleService<SimpleService> buildAnotherService() {
                return new ComplicatedSimpleService<>(newArrayList(new SimpleService("one"), new SimpleService("two")));
            }
        });

        ServiceLocator locator = initializeHK2(injector);
        WithComplicatedDependencyService service = locator.getService(WithComplicatedDependencyService.class);
        assertNotNull(service);
        assertEquals(newArrayList("just one"), service.dependency.getNames());
    }

    private ServiceLocator initializeHK2(Injector injector) {
        GuiceToHK2Adapter adapter = new GuiceToHK2Adapter(injector);
        ServiceLocatorFactory factory = ServiceLocatorFactory.getInstance();
        ServiceLocator locator = factory.create(null);
        ServiceLocatorUtilities.bind(locator, adapter);
        return locator;
    }

}
