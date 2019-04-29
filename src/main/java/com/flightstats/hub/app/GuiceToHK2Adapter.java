package com.flightstats.hub.app;

import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.name.Names;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.glassfish.hk2.api.Factory;
import org.glassfish.hk2.utilities.binding.AbstractBinder;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class GuiceToHK2Adapter extends AbstractBinder {

    private final Injector injector;

    public GuiceToHK2Adapter(Injector injector) {
        this.injector = injector;
    }

    @Override
    protected void configure() {
        injector.getAllBindings().forEach((key, value) -> bindClass(key));
    }

    private void bindClass(Key<?> key) {
        Type type = key.getTypeLiteral().getType();
        Class<?> rawType = key.getTypeLiteral().getRawType();
        Optional<String> name = getName(key);

        if (name.isPresent()) {
            bindFactoryWithName(type, rawType, name.get());
        } else {
            bindFactory(type, rawType);
        }
    }

    private void bindFactory(Type type, Class<?> rawType) {
        log.debug("mapping guice to hk2: {}", type.getTypeName());
        bindFactory(new ServiceFactory<>(rawType)).to(type);
    }

    private void bindFactoryWithName(Type type, Class<?> rawType, String name) {
        log.debug("mapping guice to hk2: {} (named: {})", type.getTypeName(), name);
        bindFactory(new ServiceFactory<>(rawType, name)).to(type).named(name);
    }

    private Optional<String> getName(Key<?> key) {
        return Optional.ofNullable(key.getAnnotationType())
                .filter(annotationType -> annotationType.getSimpleName().equals("Named"))
                .map(annotationType -> getNameAnnotation(key, annotationType));
    }

    @SneakyThrows
    private String getNameAnnotation(Key<?> key, Class<? extends Annotation> annotationType) {
        Method value = annotationType.getDeclaredMethod("value");
        return (String) value.invoke(key.getAnnotation());
    }

    private class ServiceFactory<T> implements Factory<T> {

        private final AtomicBoolean hasBeenProvided = new AtomicBoolean(false);
        private final Class<T> serviceClass;
        private final String name;

        ServiceFactory(Class<T> serviceClass) {
            this(serviceClass, null);
        }

        ServiceFactory(Class<T> serviceClass, String name) {
            this.serviceClass = serviceClass;
            this.name = name;
        }


        public T provide() {
            if (isFirstProvide()) {
                log.debug("providing {} for the first time", getPrettyName());
            }

            if (isNamed()) {
                return injector.getInstance(Key.get(serviceClass, Names.named(name)));
            } else {
                return injector.getInstance(serviceClass);
            }
        }

        private boolean isFirstProvide() {
            return hasBeenProvided.compareAndSet(false, true);
        }

        public void dispose(T versionResource) {
            // do nothing
        }

        private String getPrettyName() {
            if (isNamed()) {
                return String.format("%s (named: %s)", serviceClass.getCanonicalName(), name);
            } else {
                return serviceClass.getCanonicalName();
            }
        }

        private boolean isNamed() {
            return name != null;
        }

    }

}
