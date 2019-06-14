package com.flightstats.hub.system.extension;

import com.flightstats.hub.system.config.PropertiesLoader;
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

import javax.inject.Qualifier;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

@Slf4j
public class DependencyInjectionResolver implements ParameterResolver {
    private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace.GLOBAL;

    private static final String PROPERTY_FILE_NAME = "system-test-hub.properties";
    private static final Properties properties = new PropertiesLoader().loadProperties(PROPERTY_FILE_NAME);

    @Override
    public boolean supportsParameter(ParameterContext parameterContext,
                                     ExtensionContext extensionContext)
            throws ParameterResolutionException {
        log.info("supports parameter injection resolver");
        Parameter parameter = parameterContext.getParameter();
        if (getBindingAnnotations(parameter).size() > 1) {
            return false;
        }

        Key<?> key = getKey(
                extensionContext.getTestClass(),
                parameter);
        Optional<Injector> optInjector = getInjectorForParameterResolution(extensionContext);
        return optInjector.filter(injector -> {
            try {
                injector.getInstance(key);
                return true;
            } catch (ConfigurationException | ProvisionException e) {
                // If we throw a ParameterResolutionException here instead of returning false, we'll block
                // other ParameterResolvers from being able to work.
                return false;
            }
        }).isPresent();
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext,
                                   ExtensionContext extensionContext)
            throws ParameterResolutionException {
        log.info("parameter resolver");
        Parameter parameter = parameterContext.getParameter();
        Key<?> key = getKey(extensionContext.getTestClass(), parameter);
        Injector injector = getInjectorForParameterResolution(extensionContext)
                .orElseThrow(() ->
                        new ParameterResolutionException(
                                String.format(
                                        "Could not create injector for: %s It has no annotated element.",
                                        extensionContext.getDisplayName())));

        return injector.getInstance(key);
    }

    private static Key<?> getKey(Optional<Class<?>> containingElement, Parameter parameter) {
        Class<?> clazz =
                containingElement.orElseGet(() -> parameter.getDeclaringExecutable().getDeclaringClass());
        TypeToken<?> classType = TypeToken.of(clazz);
        Type resolvedType = classType.resolveType(parameter.getParameterizedType()).getType();

        Optional<Key<?>> key =
                getOnlyBindingAnnotation(parameter).map(annotation -> Key.get(resolvedType, annotation));
        return key.orElse(Key.get(resolvedType));
    }

    /**
     * @throws IllegalArgumentException if the given element has more than one binding
     *     annotation.
     */
    private static Optional<? extends Annotation> getOnlyBindingAnnotation(AnnotatedElement element) {
        return Optional.ofNullable(Iterables.getOnlyElement(getBindingAnnotations(element), null));
    }

    private static List<Annotation> getBindingAnnotations(AnnotatedElement element) {
        List<Annotation> annotations = new ArrayList<>();
        for (Annotation annotation : element.getAnnotations()) {
            if (isBindingAnnotation(annotation)) {
                annotations.add(annotation);
            }
        }

        return annotations;
    }

    private static boolean isBindingAnnotation(Annotation annotation) {
        Class<? extends Annotation> annotationType = annotation.annotationType();
        return annotationType.isAnnotationPresent(Qualifier.class)
                || annotationType.isAnnotationPresent(BindingAnnotation.class);
    }

    private static Optional<Injector> getInjectorForParameterResolution(
            ExtensionContext extensionContext) throws ParameterResolutionException {
        return Optional.of(extensionContext.getRoot().getStore(NAMESPACE).get("injector", Injector.class));
    }
}
