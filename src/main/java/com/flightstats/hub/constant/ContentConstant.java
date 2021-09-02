package com.flightstats.hub.constant;

public class ContentConstant {

    public static final String CREATION_DATE = "Creation-Date";

    public static final String VALID_NAME = "^[a-zA-Z0-9_-]+$";
    public static final String CONTENT_TYPE = "application/hub";

    public static final String SPOKE_TMP_SUFFIX = ".spoke.tmp";

    public static final String GET_OLDEST_ITEM_COMMAND = "find %s -not \\( -name *" + SPOKE_TMP_SUFFIX + " \\) -type f -printf '%%T+ %%p\\n' | sort | head -n 1";
    public static final String GET_ITEM_COUNT_COMMAND = "find %s -type f | wc -l";
}
