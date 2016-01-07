package com.flightstats.hub.app;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.flightstats.hub.rest.HalLinks;
import com.flightstats.hub.rest.HalLinksSerializer;
import com.flightstats.hub.rest.Rfc3339DateSerializer;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.servlet.GuiceServletContextListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

public class GuiceContext {
    private final static Logger logger = LoggerFactory.getLogger(GuiceContext.class);
    public static ObjectMapper mapper = objectMapper();

    public static HubGuiceServlet construct() {
        //todo - gfm - 1/6/16 -
        /*Map<String, String> jerseyProps = new HashMap<>();


        jerseyProps.put(FEATURE_CANONICALIZE_URI_PATH, "true");


        }*/
        //return new HubGuiceServlet(modules.toArray(new Module[modules.size()]));
        return new HubGuiceServlet();
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

    public static ObjectMapper objectMapper() {
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
