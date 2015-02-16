package com.flightstats.hub.app;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class HubVersion {
    private final static Logger logger = LoggerFactory.getLogger(HubVersion.class);

    private static String version;

    @Inject
    @Named("app.lib_path")
    String libPath;

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
            version = "v2-local";
        } catch (Exception e) {
            logger.info("unable to get version ", e);
            version = "v2";
        }
        return version;
    }
}
