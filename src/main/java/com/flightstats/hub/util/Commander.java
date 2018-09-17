package com.flightstats.hub.util;

import com.amazonaws.util.StringUtils;
import com.google.common.base.Function;
import com.google.common.io.ByteStreams;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class Commander {
    private final static Logger logger = LoggerFactory.getLogger(Commander.class);

    public static String run(String[] command, int waitTimeSeconds) {
        return process(command, waitTimeSeconds,
                (input -> {
                    try {
                        return new String(ByteStreams.toByteArray(input));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }));
    }

    public static List<String> runLines(String[] command, int waitTimeSeconds) {
        return process(command, waitTimeSeconds,
                (input -> {
                    try {
                        return IOUtils.readLines(input, StandardCharsets.UTF_8);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }));
    }

    public static String runInBash(String command, int waitTimeSeconds) {
        return run(new String[]{"/bin/bash", "-c", command}, waitTimeSeconds);
    }

    private static <T> T process(String[] command, int waitTimeSeconds, Function<InputStream, T> processor) {
        T output = null;
        try {
            logger.trace("running " + StringUtils.join(" ", command));
            long start = System.currentTimeMillis();
            Process process = new ProcessBuilder(command)
                    .redirectError(ProcessBuilder.Redirect.INHERIT)
                    .start();
            boolean waited = process.waitFor(waitTimeSeconds, TimeUnit.SECONDS);
            InputStream inputStream = new BufferedInputStream(process.getInputStream());
            output = processor.apply(inputStream);
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
