package com.flightstats.hub.util;

import com.amazonaws.util.StringUtils;
import com.google.common.io.ByteStreams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

public class FileUtil {
    private final static Logger logger = LoggerFactory.getLogger(FileUtil.class);

    public static String runCommand(String[] command, int waitTimeSeconds) {
        String output = "";
        try {
            logger.trace("running " + StringUtils.join(" ", command));
            long start = System.currentTimeMillis();
            Process process = new ProcessBuilder(command)
                    .redirectError(ProcessBuilder.Redirect.INHERIT)
                    .start();
            boolean waited = process.waitFor(waitTimeSeconds, TimeUnit.SECONDS);
            InputStream inputStream = new BufferedInputStream(process.getInputStream());
            output = new String(ByteStreams.toByteArray(inputStream));
            long time = System.currentTimeMillis() - start;
            if (waited) {
                logger.trace("waited " + waited + " for " + time);
            } else {
                logger.info("destroying after " + time + " " + StringUtils.join(" ", command));
                process.destroyForcibly();
            }
        } catch (Exception e) {
            logger.warn("unable to run command " + StringUtils.join(" ", command), e);
        }
        return output;
    }
}
