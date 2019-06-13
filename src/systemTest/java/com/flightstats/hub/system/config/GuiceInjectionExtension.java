package com.flightstats.hub.system.config;

import com.google.common.collect.Iterables;
import com.google.common.reflect.TypeToken;
import com.google.inject.BindingAnnotation;
import com.google.inject.ConfigurationException;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.ProvisionException;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.api.extension.TestInstancePostProcessor;

import javax.inject.Qualifier;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
public class GuiceInjectionExtension implements TestInstancePostProcessor {
    private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace.GLOBAL;
    private AtomicReference<String> releaseName = new AtomicReference<>();

    @Override
    public void postProcessTestInstance(Object testInstance, ExtensionContext context) throws Exception {
        log.info("guice injection extension");
        ExtensionContext.Store store = context.getRoot().getStore(NAMESPACE);
        if (store.get("injector") != null) {
            log.info("injecting stuff");
            String PROPERTY_FILE_NAME = "system-test-hub.properties";
            Properties properties = new PropertiesLoader().loadProperties(PROPERTY_FILE_NAME);

            store.put("properties", properties);
            store.put("injector", new DependencyInjector().getOrCreateInjector(context, properties));
        }
    }

}
