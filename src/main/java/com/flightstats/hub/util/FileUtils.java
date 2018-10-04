package com.flightstats.hub.util;

import org.apache.commons.lang3.StringUtils;

public class FileUtils {

    /**
     * @param path            the root directory to begin recursive deletion
     * @param waitTimeSeconds number of seconds before killing the process
     * @return number of files deleted
     */
    public static long deleteFiles(String path, int waitTimeSeconds) {
        String command = String.format("rm -rfv %s | grep \"removed '\" | wc -l", path);
        return executeAndParse(command, waitTimeSeconds);
    }

    /**
     * @param path            the root directory to begin recursive deletion
     * @param ageMinutes      number of minutes old an item needs to be for deletion
     * @param waitTimeSeconds number of seconds before killing the process
     * @return number of files deleted
     */
    public static long deleteFilesByAge(String path, int ageMinutes, int waitTimeSeconds) {
        String command = String.format("find %s -mmin +%d -exec rm -rfv {} + | grep \"removed '\" | wc -l", path, ageMinutes);
        return executeAndParse(command, waitTimeSeconds);
    }

    private static long executeAndParse(String command, int waitTimeSeconds) {
        String result = Commander.run(new String[]{"/bin/bash", "-c", command}, waitTimeSeconds);
        String value = StringUtils.chomp(result);
        return Long.parseLong(value);
    }
}
