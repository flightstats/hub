package com.flightstats.hub.util;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileUtils {

    private final static Logger logger = LoggerFactory.getLogger(FileUtils.class);

    /**
     * @param path            the root directory to begin recursive deletion
     * @param waitTimeSeconds number of seconds before killing the process
     * @return number of files deleted
     */
    public static long deleteFiles(String path, int waitTimeSeconds) {
        String command = "rm -rfv " + path + " | grep \"removed '\" | wc -l";
        return executeAndParse(command, waitTimeSeconds);
    }

    /**
     * @param path            the root directory to begin recursive deletion
     * @param ageMinutes      number of minutes old an item needs to be for deletion
     * @param waitTimeSeconds number of seconds before killing the process
     * @return number of files deleted
     */
    public static long deleteFilesByAge(String path, int ageMinutes, int waitTimeSeconds) {
        String command = "find " + path + " -mmin " + "+" + ageMinutes + " -exec rm -rfv {} + | grep \"removed '\" | wc -l";
        return executeAndParse(command, waitTimeSeconds);
    }

    private static long executeAndParse(String command, int waitTimeSeconds) {
        String result = Commander.runInBash(command, waitTimeSeconds);
        String value = StringUtils.chomp(result);
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException e) {
            throw new RuntimeException("delete command returned a non-integer: " + command);
        }
    }
}
