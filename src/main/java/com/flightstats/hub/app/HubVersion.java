package com.flightstats.hub.app;

import com.flightstats.hub.config.AppProperties;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import javax.inject.Inject;
import java.io.File;

@Slf4j
public class HubVersion {

    private final AppProperties appProperties;
    private static String version;

    @Inject
    public HubVersion(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    public synchronized String getVersion() {
        if (version != null) {
            return version;
        }
        try {
            final File libDir = new File(appProperties.getAppLibPath());
            final File[] files = libDir.listFiles((dir, name) -> name.startsWith("hub"));
            if (files.length == 1) {
                final String name = files[0].getName();
                version = StringUtils.removeEnd(StringUtils.removeStart(name, "hub-"), ".jar");
                if (version.equals("null")) {
                    throw new NullPointerException();
                }
            } else if (files.length == 0) {
                version = "no hub jar file found";
            } else {
                final StringBuilder stringBuilder = new StringBuilder();
                for (File file : files) {
                    stringBuilder.append(file.getName()).append(";");
                }
                version = "multiple hub jar files found: " + stringBuilder.toString();
            }
        } catch (NullPointerException e) {
            log.info("unable to get version, presume local");
            version = "local";
        } catch (Exception e) {
            log.info("unable to get version ", e);
            version = "hub";
        }
        return version;
    }
}