package com.flightstats.hub.spoke;

import com.flightstats.hub.metrics.ActiveTraces;
import com.flightstats.hub.metrics.Traces;
import com.flightstats.hub.model.Content;
import com.flightstats.hub.model.ContentKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SpokeBatchContentDao extends SpokeContentDao {

    protected static Logger logger = LoggerFactory.getLogger(SpokeBatchContentDao.class);

    @Override
    public Content get(String channelName, ContentKey key) {
        String path = getPath(channelName, key);
        Traces traces = ActiveTraces.getLocal();
        traces.add("SpokeBatchContentDao.get");
        try {
            return spokeStore.get("batch", path, key);
        } catch (Exception e) {
            logger.warn("unable to get data: " + path, e);
            return null;
        } finally {
            traces.add("SpokeBatchContentDao.get completed");
        }
    }

}
