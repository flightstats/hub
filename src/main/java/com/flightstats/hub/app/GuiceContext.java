package com.flightstats.hub.app;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import com.flightstats.hub.rest.HalLinks;
import com.flightstats.hub.rest.HalLinksSerializer;
import com.flightstats.hub.rest.Rfc3339DateSerializer;
import com.google.inject.*;
import com.google.inject.name.Names;
import com.google.inject.servlet.GuiceServletContextListener;
import com.sun.jersey.api.container.filter.GZIPContentEncodingFilter;
import com.sun.jersey.api.json.JSONConfiguration;
import com.sun.jersey.guice.JerseyServletModule;
import com.sun.jersey.guice.spi.container.servlet.GuiceContainer;

import javax.ws.rs.ext.ContextResolver;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static com.sun.jersey.api.core.PackagesResourceConfig.*;

public class GuiceContext {

    public static HubGuiceServlet construct() {
        Map<String, String> jerseyProps = new HashMap<>();
        jerseyProps.put(PROPERTY_PACKAGES, "com.flightstats.hub");
        jerseyProps.put(PROPERTY_CONTAINER_RESPONSE_FILTERS, GZIPContentEncodingFilter.class.getName());

        jerseyProps.put(JSONConfiguration.FEATURE_POJO_MAPPING, "true");
        jerseyProps.put(FEATURE_CANONICALIZE_URI_PATH, "true");
        jerseyProps.put(PROPERTY_CONTAINER_REQUEST_FILTERS, GZIPContentEncodingFilter.class.getName() +
                ";" + RemoveSlashFilter.class.getName());

        JerseyServletModule jerseyModule = new JerseyServletModule() {
            @Override
            protected void configureServlets() {
                Names.bindProperties(binder(), HubProperties.getProperties());
                ObjectMapper mapper = objectMapper();
                bind(ObjectMapper.class).toInstance(mapper);
                bind(ObjectMapperResolver.class).toInstance(new ObjectMapperResolver(mapper));
                bind(JacksonJsonProvider.class).in(Scopes.SINGLETON);
                serve("/*").with(GuiceContainer.class, jerseyProps);
            }
        };
        GuiceBindings guiceBindings = new GuiceBindings();

        return new HubGuiceServlet(jerseyModule, guiceBindings);
    }

    public static class HubGuiceServlet extends GuiceServletContextListener {
        private final Module[] modules;
        private Injector injector;

        public HubGuiceServlet(Module... modules) {
            this.modules = modules;
        }

        @Override
        public synchronized Injector getInjector() {
            if (injector == null) {
                injector = Guice.createInjector(modules);
            }
            return injector;
        }
    }

    @javax.ws.rs.ext.Provider
    @Singleton
    static class ObjectMapperResolver implements ContextResolver<ObjectMapper> {
        private final ObjectMapper objectMapper;

        public ObjectMapperResolver(ObjectMapper objectMapper) {
            this.objectMapper = objectMapper;
        }

        @Override
        public ObjectMapper getContext(Class<?> type) {
            return objectMapper;
        }
    }

    private static ObjectMapper objectMapper() {
        SimpleModule module = new SimpleModule();
        module.addSerializer(HalLinks.class, new HalLinksSerializer());
        module.addSerializer(Date.class, new Rfc3339DateSerializer());
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(module);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
        return mapper;
    }
}
