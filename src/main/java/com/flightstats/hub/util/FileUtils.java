package com.flightstats.hub.util;

import org.apache.commons.lang3.StringUtils;

public class FileUtils {

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
        return Long.valueOf(value);
    }
}
