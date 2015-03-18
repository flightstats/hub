package com.flightstats.hub.dao.nas;

import com.flightstats.hub.app.HubProperties;
import org.apache.commons.lang3.StringUtils;

public class NasUtil {

    public static String getStoragePath() {
        return StringUtils.appendIfMissing(HubProperties.getProperty("nas.storage.path", "/nas"), "/");
    }
}
