package com.flightstats.hub.spoke;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


class SpokePathUtil {
    private static final Pattern yearPattern = Pattern.compile("/(20\\d\\d)");
    private static final Pattern monthPattern = Pattern.compile("/20\\d\\d/(\\d\\d)");
    private static final Pattern dayPattern = Pattern.compile("/20\\d\\d/\\d\\d/(\\d\\d)");
    private static final Pattern hourPattern = Pattern.compile("/20\\d\\d/\\d\\d/\\d\\d/(\\d\\d)");
    private static final Pattern minutePattern = Pattern.compile("/20\\d\\d/\\d\\d/\\d\\d/\\d\\d/(\\d\\d)");
    private static final Pattern secondPattern = Pattern.compile("/20\\d\\d/\\d\\d/\\d\\d/\\d\\d/\\d\\d/(\\d\\d)");

    //bc: all of these patterns are anchored off of having a yyyy that starts with 20.
    // this code will need to be updated in 85 years.

    private static String timePart(String pathPart, Pattern pattern) {
        Matcher m = pattern.matcher(pathPart);
        if (m.find()) {
            return m.group(1);
        }
        return null;
    }

    static public String year(String pathPart) {
        return timePart(pathPart, yearPattern);
    }

    static public String month(String pathPart) {
        return timePart(pathPart, monthPattern);
    }

    static public String day(String pathPart) {
        return timePart(pathPart, dayPattern);
    }

    static public String hour(String pathPart) {
        return timePart(pathPart, hourPattern);
    }

    static public String minute(String pathPart) {
        return timePart(pathPart, minutePattern);
    }

    static public String second(String pathPart) {
        return timePart(pathPart, secondPattern);
    }
//    static public int millisecond(String pathPart){
//        return timePart(pathPart, millisecondPattern);
//    }


    static String smallestTimeResolution(String pathPart) {
        if (second(pathPart) != null) return "second";
        if (minute(pathPart) != null) return "minute";
        if (hour(pathPart) != null) return "hour";
        if (day(pathPart) != null) return "day";
        if (month(pathPart) != null) return "month";
        if (year(pathPart) != null) return "year";
        return null;
    }

    static String secondPathPart(String p) {
        // return path up to and including the second part
        String secPath = hourPathPart(p) + "/" + SpokePathUtil.minute(p) + "/" + SpokePathUtil.second(p);
        int i = p.lastIndexOf(secPath);
        return p.substring(0, i + secPath.length());
    }

    private static String hourPathPart(String p) {
        // return path up to and including the hour part
        String hourPath = SpokePathUtil.year(p) + "/" + SpokePathUtil.month(p) + "/" + SpokePathUtil.day(p) + "/" +
                SpokePathUtil.hour(p);
        int i = p.lastIndexOf(hourPath);
        return p.substring(0, i + hourPath.length());
    }

    static public File hourPathFolder(File path) {
        String hourPath = hourPathPart(path.getAbsolutePath());
        return new File(hourPath);
    }

}
