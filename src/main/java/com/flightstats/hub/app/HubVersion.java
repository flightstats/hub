package com.flightstats.hub.app;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.File;

@Singleton
public class HubVersion {
    private final static Logger logger = LoggerFactory.getLogger(HubVersion.class);

    private static String version;
    private final String libPath;

    @Inject
    HubVersion(HubProperties hubProperties) {
        this.libPath = hubProperties.getProperty("app.lib_path");
    }

    public synchronized String getVersion() {
        if (version != null) {
            return version;
        }
        try {
            File libDir = new File(libPath);
            File[] files = libDir.listFiles((dir, name) -> {
                return name.startsWith("hub");
            });
            if (files.length == 1) {
                String name = files[0].getName();
                version = StringUtils.removeEnd(StringUtils.removeStart(name, "hub-"), ".jar");
                if (version.equals("null")) {
                    throw new NullPointerException();
                }
            } else if (files.length == 0) {
                version = "no hub jar file found";
            } else {
                String fileNames = "";
                for (File file : files) {
                    fileNames += file.getName() + ";";
                }
                version = "multiple hub jar files found: " + fileNames;
            }
        } catch (NullPointerException e) {
            logger.info("unable to get version, presume local");
            version = "local";
        } catch (Exception e) {
            logger.info("unable to get version ", e);
            version = "hub";
        }
        return version;
    }
}
