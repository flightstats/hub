package com.flightstats.jerseyguice;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.jersey.InstrumentedResourceMethodDispatchAdapter;
import com.conducivetech.services.common.util.collections.Pair;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import com.flightstats.jerseyguice.jetty.health.DefaultHealthCheck;
import com.flightstats.jerseyguice.jetty.health.HealthCheck;
import com.flightstats.jerseyguice.mapper.UnrecognizedPropertyExceptionMapper;
import com.flightstats.jerseyguice.metrics.GraphiteConfig;
import com.flightstats.jerseyguice.metrics.MethodTimingAdapterProvider;
import com.flightstats.jerseyguice.metrics.MetricsReporting;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.inject.Module;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import com.google.inject.name.Names;
import com.sun.jersey.guice.JerseyServletModule;
import com.sun.jersey.guice.spi.container.servlet.GuiceContainer;
import com.sun.jersey.spi.container.ContainerRequestFilter;
import com.sun.jersey.spi.container.ContainerResponseFilter;
import org.jetbrains.annotations.NotNull;

import javax.servlet.http.HttpServlet;
import java.util.*;

import static com.flightstats.jerseyguice.metrics.GraphiteConfig.NO_GRAPHITE;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.sun.jersey.api.core.PackagesResourceConfig.PROPERTY_PACKAGES;
import static com.sun.jersey.api.core.ResourceConfig.PROPERTY_CONTAINER_REQUEST_FILTERS;
import static com.sun.jersey.api.core.ResourceConfig.PROPERTY_CONTAINER_RESPONSE_FILTERS;

/**
 * todo - gfm - 3/20/14 - This overrides JerseyServletModuleBuilder in jerseyguice.  It can be deleted once the lib is
 * updated to have these changes.
 */
public class JerseyServletModuleBuilder {
    private final Map<String, String> jerseyProperties = new HashMap<>();
    private final Set<String> jerseyPackages = new LinkedHashSet<>();
    private final List<Module> modules = Lists.newArrayList();

    private Optional<Properties> namedProperties = Optional.absent();

    private GraphiteConfig graphiteConfig = NO_GRAPHITE;
    private Bindings bindings = new NoAdditionalBindings();
    private Class<? extends HealthCheck> healthCheckClass = DefaultHealthCheck.class;

    private ObjectMapper objectMapper;

    private String path = "/*";
    private Pair<String, Class<? extends HttpServlet>> regexServe;

    private final List<String> containerRequestFilters = Lists.newArrayList();
    private final List<String> containerResponseFilters = Lists.newArrayList();
    private boolean enableJerseyGuiceResources = true;

    public JerseyServletModuleBuilder() {
    }

    /**
     * Adds a package that should be inspected for Jersey annotations.
     * This can be called multiple times to set multiple packages.
     *
     * @param packageName the name of the package to inspect for Jersey annotations (e.g. "com.flightstats.service")
     */
    public JerseyServletModuleBuilder withJerseyPackage(@NotNull final String packageName) {
        jerseyPackages.add(packageName);
        return this;
    }

    /**
     * Adds a custom property to the Jersey configuration.
     * Note that the packages to be scanned for Jersey annotations should be specified using the {@link #withJerseyPackage(String)} method.
     *
     * @param propertyName  the name of the Jersey property
     * @param propertyValue the value of the Jersey property
     */
    public JerseyServletModuleBuilder withJerseryProperty(
            @NotNull final String propertyName,
            @NotNull final String propertyValue) {
        checkNotNull(propertyValue);

        switch (propertyName) {
            case PROPERTY_PACKAGES:
                throw new IllegalArgumentException("Do not call withJerseyProperty() to set" +
                        " 'PackagesResourceConfig.PROPERTY_PACKAGES', instead use withJerseyPackage()");
            case PROPERTY_CONTAINER_REQUEST_FILTERS:
                throw new IllegalArgumentException("Do not call withJerseyProperty() to set" +
                        " 'PackagesResourceConfig.PROPERTY_CONTAINER_REQUEST_FILTERS', instead use withJerseyRequestFilters()");
            case PROPERTY_CONTAINER_RESPONSE_FILTERS:
                throw new IllegalArgumentException("Do not call withJerseyProperty() to set" +
                        " 'PackagesResourceConfig.PROPERTY_CONTAINER_RESPONSE_FILTERS', instead use withJerseyResponseFilters()");
            default:
                jerseyProperties.put(propertyName, propertyValue);
        }
        return this;
    }

    /**
     * Add the specified request filters to the jersey configuration. Filters will be applied in the
     * order in which they appear in the method parameter. Successive calls to this method will not
     * overwrite filters set with previous calls.
     *
     * @param filterClasses the filter classes
     * @return this builder
     */
    @SafeVarargs
    public final JerseyServletModuleBuilder withContainerRequestFilters(Class<? extends ContainerRequestFilter>... filterClasses) {
        for (Class<? extends ContainerRequestFilter> filterClass : filterClasses) {
            containerRequestFilters.add(filterClass.getName());
        }
        return this;
    }

    /**
     * Add the specified response filters to the jersey configuration. Filters will be applied in the
     * order in which they appear in the method parameter. Successive calls to this method will not
     * overwrite filters set with previous calls.
     *
     * @param filterClasses the filter classes
     * @return this builder
     */
    @SafeVarargs
    public final JerseyServletModuleBuilder withContainerResponseFilters(Class<? extends ContainerResponseFilter>... filterClasses) {
        for (Class<? extends ContainerResponseFilter> filterClass : filterClasses) {
            containerResponseFilters.add(filterClass.getName());
        }
        return this;
    }

    /**
     * @param path the base path to be served by Jersey, defaults to "/*" if not specified.
     */
    public JerseyServletModuleBuilder withServletPath(@NotNull final String path) {
        this.path = path;
        return this;
    }

    /**
     * @param config the GraphiteConfig to use to wire metrics emission and enable use of the "@Timed" annotation on Jersey end-points
     */
    public JerseyServletModuleBuilder withGraphiteConfig(@NotNull final GraphiteConfig config) {
        this.graphiteConfig = config;
        return this;
    }

    /**
     * Sets up a callback to register additional bindings in Guice.
     * If not specified the "NoAdditionalBindings" implementation will be used.
     *
     * @param bindings callback to register additional bindings
     */
    public JerseyServletModuleBuilder withBindings(@NotNull final Bindings bindings) {
        this.bindings = bindings;
        return this;
    }

    /**
     * Add the specified modules to the list of those to be installed performing module
     * configuration.
     *
     * @param modules the modules to install
     */
    public JerseyServletModuleBuilder withModules(@NotNull final Collection<Module> modules) {
        this.modules.addAll(modules);
        return this;
    }


    /**
     * @param properties a set of Properties to be exposed via the @Named annotations
     */
    public JerseyServletModuleBuilder withNamedProperties(@NotNull final Properties properties) {
        namedProperties = Optional.fromNullable(properties);
        return this;
    }

    /**
     * @param mapper the ObjectMapper to be used for
     */
    public JerseyServletModuleBuilder withObjectMapper(@NotNull final ObjectMapper mapper) {
        this.objectMapper = mapper;
        return this;
    }

    /**
     * @param regex The URL regex to be served.
     * @param clazz The class to call.
     */
    public JerseyServletModuleBuilder withRegexServe(@NotNull final String regex, Class<? extends HttpServlet> clazz) {
        this.regexServe = new Pair<String, Class<? extends HttpServlet>>(regex, clazz);
        return this;
    }

    /**
     * @param healthCheckClass the class implementing HealthCheck, if not supplied the DefaultHealthCheck.class will be used
     */
    public JerseyServletModuleBuilder withHealthCheckClass(@NotNull final Class<? extends HealthCheck> healthCheckClass) {
        this.healthCheckClass = healthCheckClass;
        return this;
    }

    /**
     * @param enableJerseyGuiceResources if false, turn off Resources from com.flightstats.jerseyguice
     */
    public JerseyServletModuleBuilder withJerseyGuiceResources(boolean enableJerseyGuiceResources) {
        this.enableJerseyGuiceResources = enableJerseyGuiceResources;
        return this;
    }

    public JerseyServletModule build() {
        checkArgument(objectMapper != null, "Unable to construct JerseyServletModule" +
                ", no ObjectMapper was configured");
        checkArgument(!jerseyPackages.isEmpty(), "Unable to construct JerseyServletModule," +
                " at least one jersey package must be specified");

        final Map<String, String> jerseyProps = getJerseyProperties();

        return new JerseyServletModule() {
            @Override
            protected void configureServlets() {
                // Maybe bind the named properties if given
                if (namedProperties.isPresent()) {
                    Names.bindProperties(binder(), namedProperties.get());
                }
                bind(UnrecognizedPropertyExceptionMapper.class).asEagerSingleton();

                // Bind the ObjectMapper and the Jersey/Jackson sidekicks
                bind(ObjectMapper.class).toInstance(objectMapper);
                bind(ObjectMapperResolver.class).toInstance(new ObjectMapperResolver(objectMapper));
                bind(JacksonJsonProvider.class).in(Scopes.SINGLETON);

                if (shouldBuildGraphite()) {
                    // Bind the MetricsRegistry and reporting, add the timing adapter
                    bind(GraphiteConfig.class).toInstance(graphiteConfig);
                    bind(MetricRegistry.class).in(Singleton.class);
                    bind(MetricsReporting.class).asEagerSingleton();
                    bind(InstrumentedResourceMethodDispatchAdapter.class).toProvider(MethodTimingAdapterProvider.class).in(Singleton.class);
                }

                // Do any additional bindings
                bindings.bind(binder());

                // Install any additional modules
                for (Module module : modules) {
                    install(module);
                }

                // Bind the health check
                bind(HealthCheck.class).to(healthCheckClass);

                if (regexServe != null) {
                    serveRegex(regexServe.getLeft()).with(regexServe.getRight());
                }
                serve(path).with(GuiceContainer.class, jerseyProps);
            }
        };
    }

    @VisibleForTesting
    Map<String, String> getJerseyProperties() {
        // Ensure we include our health check in the packages to be served
        final List<String> propertyPackages = new ArrayList<>(jerseyPackages);
        if (enableJerseyGuiceResources) {
            propertyPackages.add("com.flightstats.jerseyguice");
        }

        // Make a copy of the jersey properties as a new map and add the packages specified for REST endpoints
        ImmutableMap.Builder<String, String> builder = ImmutableMap.builder();
        builder.putAll(jerseyProperties);

        Joiner joiner = Joiner.on(';');
        builder.put(PROPERTY_PACKAGES, joiner.join(propertyPackages));

        if (!containerRequestFilters.isEmpty()) {
            builder.put(PROPERTY_CONTAINER_REQUEST_FILTERS, joiner.join(containerRequestFilters));
        }
        if (!containerResponseFilters.isEmpty()) {
            builder.put(PROPERTY_CONTAINER_RESPONSE_FILTERS, joiner.join(containerResponseFilters));
        }

        return builder.build();
    }

    private boolean shouldBuildGraphite() {
        return !NO_GRAPHITE.equals(graphiteConfig);
    }
}
