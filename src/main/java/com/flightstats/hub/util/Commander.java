package com.flightstats.hub.util;

import com.amazonaws.util.StringUtils;
import com.google.common.base.Function;
import com.google.common.io.ByteStreams;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
public class Commander {

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

    private static <T> T process(String[] command, int waitTimeSeconds, Function<InputStream, T> processor) {
        T output = null;
        try {
            log.trace("running " + StringUtils.join(" ", command));
            long start = System.currentTimeMillis();
            Process process = new ProcessBuilder(command)
                    .redirectError(ProcessBuilder.Redirect.INHERIT)
                    .start();
            boolean waited = process.waitFor(waitTimeSeconds, TimeUnit.SECONDS);
            InputStream inputStream = new BufferedInputStream(process.getInputStream());
            output = processor.apply(inputStream);
            long time = System.currentTimeMillis() - start;
            if (waited) {
                log.trace("waited " + waited + " for " + time);
            } else {
                log.warn("destroying after " + time + " " + StringUtils.join(" ", command));
                process.destroyForcibly();
            }
        } catch (Exception e) {
            log.warn("unable to run command " + StringUtils.join(" ", command), e);
        }
        return output;
    }

    public String runInBash(String command, int waitTimeSeconds) {
        return run(new String[]{"/bin/bash", "-c", command}, waitTimeSeconds);
    }
}
