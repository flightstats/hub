package com.flightstats.hub;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

@Slf4j
public class PropertyLoader {

    public static Properties load(String propertyFilename) {
        final Properties properties = new Properties();
        try (final InputStream inputStream =
                     PropertyLoader.class.getClassLoader().getResourceAsStream(propertyFilename)) {
            properties.load(inputStream);

        } catch (IOException e) {
            log.error("Property file {} not found", propertyFilename, e);
        }
        return properties;
    }
}
