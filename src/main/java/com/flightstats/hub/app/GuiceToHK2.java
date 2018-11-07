package com.flightstats.hub.app;

import com.google.inject.Injector;
import lombok.extern.slf4j.Slf4j;
import org.glassfish.hk2.api.Factory;
import org.glassfish.hk2.utilities.binding.AbstractBinder;

@Slf4j
class GuiceToHK2 extends AbstractBinder {

    private final Injector injector;

    GuiceToHK2(Injector injector) {
        this.injector = injector;
    }

    @Override
    protected void configure() {
        injector.getBindings().forEach((key, value) -> {
            String typeName = key.getTypeLiteral().getType().getTypeName();
            if (!typeName.startsWith("com.flightstats.hub")) return;
            log.debug("mapping guice to hk2: {}", typeName);
            try {
                Class boundClass = Class.forName(typeName);
                bindFactory(new ServiceFactory<>(boundClass)).to(boundClass);
            } catch (ClassNotFoundException e) {
                log.warn("unable to find class: {}", typeName);
            }
        });
    }

    private class ServiceFactory<T> implements Factory<T> {

        private final Class<T> serviceClass;

        ServiceFactory(Class<T> serviceClass) {
            this.serviceClass = serviceClass;
        }

        public T provide() {
            return injector.getInstance(serviceClass);
        }

        public void dispose(T versionResource) {
            // do nothing
        }
    }

}
