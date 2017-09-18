package com.flightstats.hub.spoke;

import com.flightstats.hub.metrics.ActiveTraces;
import com.flightstats.hub.metrics.Traces;
import com.flightstats.hub.model.Content;
import com.flightstats.hub.model.ContentKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SpokeSingleContentDao extends SpokeContentDao {

    protected static Logger logger = LoggerFactory.getLogger(SpokeSingleContentDao.class);

    @Override
    public Content get(String channelName, ContentKey key) {
        String path = getPath(channelName, key);
        Traces traces = ActiveTraces.getLocal();
        traces.add("SpokeSingleContentDao.get");
        try {
            return spokeStore.get("single", path, key);
        } catch (Exception e) {
            logger.warn("unable to get data: " + path, e);
            return null;
        } finally {
            traces.add("SpokeSingleContentDao.get completed");
        }
    }

}
