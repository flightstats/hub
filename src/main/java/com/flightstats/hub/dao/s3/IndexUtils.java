package com.flightstats.hub.dao.s3;

import com.flightstats.hub.model.ContentKey;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class IndexUtils {

    static Collection<ContentKey> convertIds(List<String> ids) {
        List<ContentKey> keys = new ArrayList<>();
        for (String id : ids) {
            keys.add(ContentKey.fromString(id).get());
        }
        return keys;
    }
}
