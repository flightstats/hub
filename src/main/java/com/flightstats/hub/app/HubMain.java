package com.flightstats.hub.app;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Stage;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

@Slf4j
public class HubMain {

    public static void main(String... args) throws Exception {
        Properties properties = loadProperties(args[0]);
        if (shouldRunZooKeeperInProcess(properties)) {
            new Thread(() -> new ZooKeeperInProcess().start()).start();
        }
        Injector injector = Guice.createInjector(Stage.PRODUCTION, buildModuleArray(properties));
        injector.getInstance(HubApplication.class).run();
    }

    private static Properties loadProperties(String path) throws IOException {
        log.info("loading properties file: {}", path);
        Properties properties = new Properties();
        InputStream propertiesStream = new FileInputStream(path);
        properties.load(propertiesStream);
        return properties;
    }

    private static boolean shouldRunZooKeeperInProcess(Properties properties) {
        String property = properties.getProperty("runSingleZookeeperInternally", "");
        return !StringUtils.isEmpty(property);
    }

    private static AbstractModule[] buildModuleArray(Properties properties) {
        List<AbstractModule> modules = new ArrayList<>();
        modules.add(new HubModule(properties));
        String type = properties.getProperty("hub.type", "");
        if (type.equals("aws")) {
            modules.add(new ClusteredModule());
        } else {
            modules.add(new StandaloneModule());
        }
        log.debug("using Guice modules: {}", getModuleNames(modules));
        return modules.toArray(new AbstractModule[0]);
    }

    private static String getModuleNames(List<AbstractModule> modules) {
        return modules.stream()
                .map(module -> module.getClass().getSimpleName())
                .collect(Collectors.joining(", "));
    }

}
