package com.flightstats.hub.spoke;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by bcorliss on 11/20/14.
 */
public class SpokePathUtil {
    public static final Pattern yearPattern =   Pattern.compile("/(20\\d\\d)");
    public static final Pattern monthPattern =  Pattern.compile("/20\\d\\d/(\\d\\d)");
    public static final Pattern dayPattern =    Pattern.compile("/20\\d\\d/\\d\\d/(\\d\\d)");
    public static final Pattern hourPattern =   Pattern.compile("/20\\d\\d/\\d\\d/\\d\\d/(\\d\\d)");
    public static final Pattern minutePattern = Pattern.compile("/20\\d\\d/\\d\\d/\\d\\d/\\d\\d/(\\d\\d)");
    public static final Pattern secondPattern = Pattern.compile("/20\\d\\d/\\d\\d/\\d\\d/\\d\\d/\\d\\d/(\\d\\d)");
    public static final Pattern secondPathPartPattern =
            Pattern.compile("(/20\\d\\d/\\d\\d/\\d\\d/\\d\\d/\\d\\d/\\d\\d)");
//    public static final Pattern millisecondPattern = Pattern.compile
//                                                                ("/20\\d\\d/\\d\\d/\\d\\d/\\d\\d/\\d\\d/\\d\\d[/]\\?" +
//                                                                        "(\\d\\d\\d)");
//bc: all of these patterns are anchored off of having a yyyy that starts with 20.
    // this code will need to be updated in 85 years.

    static public String timePart(String pathPart, Pattern pattern){
        Matcher m = pattern.matcher(pathPart);
        if(m.find()) {
           return m.group(1);
        }
        return null;
    }
    static public String year(String pathPart){
        return timePart(pathPart, yearPattern);
    }
    static public String month(String pathPart){
        return timePart(pathPart, monthPattern);
    }
    static public String day(String pathPart){
        return timePart(pathPart, dayPattern);
    }
    static public String hour(String pathPart){
        return timePart(pathPart, hourPattern);
    }
    static public String minute(String pathPart){
        return timePart(pathPart, minutePattern);
    }
    static public String second(String pathPart){
        return timePart(pathPart, secondPattern);
    }
//    static public int millisecond(String pathPart){
//        return timePart(pathPart, millisecondPattern);
//    }

    static public String smallestTimeResolution(String pathPart){
        String result;
//        if(millisecond(pathPart)>-1) return "millisecond";
        if(second(pathPart)!=null) return "second";
        if(minute(pathPart)!=null) return "minute";
        if(hour(pathPart)!=null) return "hour";
        if(day(pathPart)!=null) return "day";
        if(month(pathPart)!=null) return "month";
        if(year(pathPart)!=null) return "year";
        return null;
    }

    static public String secondPathPart(String p){
        // return path up to and including the second part
        String secPath = SpokePathUtil.year(p)+"/"+ SpokePathUtil.month(p)+"/"+ SpokePathUtil.day(p)+"/"+ SpokePathUtil.hour(p)+
                "/"+ SpokePathUtil.minute(p)+"/"+ SpokePathUtil.second(p);
        int i = p.lastIndexOf(secPath);
        return p.substring(0,i + secPath.length());
    }

}
