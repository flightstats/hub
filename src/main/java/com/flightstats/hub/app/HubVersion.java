package com.flightstats.hub.app;

import com.flightstats.hub.config.properties.AppProperties;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import javax.inject.Inject;
import java.io.File;

@Slf4j
public class HubVersion {

    @Inject
    private AppProperties appProperties;

    private static String version;

    public synchronized String getVersion() {
        if (version != null) {
            return version;
        }
        try {
            File libDir = new File(appProperties.getAppLibPath());
            File[] files = libDir.listFiles((dir, name) -> name.startsWith("hub"));
            if (files.length == 1) {
                String name = files[0].getName();
                version = StringUtils.removeEnd(StringUtils.removeStart(name, "hub-"), ".jar");
                if (version.equals("null")) {
                    throw new NullPointerException();
                }
            } else if (files.length == 0) {
                version = "no hub jar file found";
            } else {
                StringBuilder stringBuilder = new StringBuilder();
                for (File file : files) {
                    stringBuilder.append(file.getName()).append(";");
                }
                version = "multiple hub jar files found: " + stringBuilder.toString();
            }
        } catch (NullPointerException e) {
            log.warn("unable to get version, presume local", e);
            version = "local";
        } catch (Exception e) {
            log.warn("unable to get version ", e);
            version = "hub";
        }
        return version;
    }
}