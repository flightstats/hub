package com.flightstats.hub.dao.s3;

import com.flightstats.hub.cluster.CuratorLeader;
import com.flightstats.hub.cluster.DateTimeValue;
import com.flightstats.hub.cluster.Leader;
import com.flightstats.hub.dao.ContentDao;
import com.flightstats.hub.model.ChannelContentKey;
import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.model.Traces;
import com.flightstats.hub.util.Sleeper;
import com.flightstats.hub.util.TimeUtil;
import org.apache.curator.framework.CuratorFramework;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * This should make sure that all data written to Spoke eventually gets written to S3.
 * We should also verify that the data does get written with no "black holes"
 */
public class S3ChannelWriter implements Leader {
    private final static Logger logger = LoggerFactory.getLogger(S3ChannelWriter.class);

    private final ContentDao cacheContentDao;
    private final S3WriteQueue s3WriteQueue;
    private final CuratorFramework curator;
    private final DateTimeValue lastCompleted;
    private String channel;

    private CuratorLeader curatorLeader;

    public S3ChannelWriter(ContentDao cacheContentDao,
                           S3WriteQueue s3WriteQueue,
                           CuratorFramework curator) {
        this.cacheContentDao = cacheContentDao;
        this.s3WriteQueue = s3WriteQueue;
        this.curator = curator;
        lastCompleted = new DateTimeValue(curator);
    }

    static long getSleep(DateTime lastTime, DateTime now) {
        long difference = now.getMillis() - lastTime.getMillis();
        long sleep = 70 * 1000 - difference;
        if (sleep < 0) {
            return 0;
        }
        return sleep;
    }

    public boolean tryLeadership(String channel) {
        logger.debug("trying leadership for channel {} ", channel);
        this.channel = channel;
        lastCompleted.initialize(getValuePath(), TimeUtil.now());
        curatorLeader = new CuratorLeader(getLeaderPath(), this, curator);
        curatorLeader.start();
        return true;
    }

    @Override
    public void takeLeadership(AtomicBoolean hasLeadership) {
        while (hasLeadership.get()) {
            DateTime nextTime = lastCompleted.get(getValuePath(), TimeUtil.now()).plusMinutes(1);
            Sleeper.sleep(getSleep(nextTime, TimeUtil.now()));
            logger.debug("processing {} {} ", channel, nextTime);
            Collection<ContentKey> contentKeys = cacheContentDao.queryByTime(channel, nextTime, TimeUtil.Unit.MINUTES, Traces.NOOP);
            for (ContentKey contentKey : contentKeys) {
                s3WriteQueue.add(new ChannelContentKey(channel, contentKey));
            }
            lastCompleted.update(getValuePath(), nextTime);
        }
    }

    public void close() {
        logger.info("closing {} ", channel);
        try {
            if (curatorLeader != null) {
                curatorLeader.close();
            }
        } catch (Exception e) {
            logger.warn("unable to close curatorLeader " + channel, e);
        }
    }

    private String getValuePath() {
        return "/S3ChannelWriter/LastCompleted/" + channel;
    }

    private String getLeaderPath() {
        return "/S3ChannelWriter/Leader/" + channel;
    }


}
