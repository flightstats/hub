package com.flightstats.hub.dao.s3;

import com.flightstats.hub.app.HubServices;
import com.flightstats.hub.dao.ContentDao;
import com.flightstats.hub.dao.timeIndex.TimeIndex;
import com.flightstats.hub.model.ChannelConfiguration;
import com.flightstats.hub.model.Content;
import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.model.InsertedContentKey;
import com.google.common.base.Optional;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.inject.Inject;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;

/**
 * This uses S3 for Content and ZooKeeper/S3 for TimeIndex
 */
public class ContentDaoImpl implements ContentDao {

    private final static Logger logger = LoggerFactory.getLogger(ContentDaoImpl.class);

    private final ZooKeeperIndexDao zooKeeperIndexDao;
    private final S3ContentDao s3ContentDao;
    private final S3IndexDao s3IndexDao;

    @Inject
    public ContentDaoImpl(ZooKeeperIndexDao zooKeeperIndexDao,
                          S3ContentDao s3ContentDao,
                          S3IndexDao s3IndexDao) {
        this.s3ContentDao = s3ContentDao;
        this.zooKeeperIndexDao = zooKeeperIndexDao;
        this.s3IndexDao = s3IndexDao;
        HubServices.register(new S3ContentDaoInit());
    }

    @Override
    public InsertedContentKey write(String channelName, Content content, long ttlDays) {
        if (content.isNewContent()) {
            content.setContentKey(new ContentKey());
        } else {
            //todo - gfm - 10/31/14 - how should replication be handled?
        }
        ContentKey key = content.getContentKey().get();
        DateTime dateTime = new DateTime(content.getMillis());
        s3ContentDao.writeS3(channelName, content, key);
        zooKeeperIndexDao.writeIndex(channelName, dateTime, key);
        return new InsertedContentKey(key, dateTime.toDate());
    }

    @Override
    public Content read(final String channelName, final ContentKey key) {
        return s3ContentDao.read(channelName, key);
    }

    @Override
    public Collection<ContentKey> getKeys(String channelName, DateTime dateTime) {
        /**
         * The TimeIndex is written to ZK, then TimeIndexProcessor write the data to S3, then deletes the keys.
         * We try reading from S3 first, because the ZK cache may be partial if it is already written to S3.
         * There is a bit of a race condition here, especially with S3 eventual consistency.
         */
        String hashTime = TimeIndex.getHash(dateTime);
        try {
            return s3IndexDao.getKeys(channelName, hashTime);
        } catch (Exception e) {
            logger.debug("unable to find keys in S3 " + channelName + hashTime + e.getMessage());
        }
        try {
            return zooKeeperIndexDao.getKeys(channelName, hashTime);
        } catch (Exception e) {
            logger.debug("unable to find keys in ZK " + channelName + hashTime + e.getMessage());
        }
        return Collections.emptyList();
    }

    @Override
    public void initializeChannel(ChannelConfiguration config) {

        //todo - gfm - 10/31/14 - does this need to create the table in Riak?
    }

    @Override
    public Optional<ContentKey> getKey(String id) {
        return ContentKey.fromString(id);
    }

    @Override
    public void delete(String channelName) {
        s3ContentDao.delete(channelName);
        //todo - gfm - 10/31/14 - delete this too...
        zooKeeperIndexDao.delete(channelName);
    }

    private class S3ContentDaoInit extends AbstractIdleService {
        @Override
        protected void startUp() throws Exception {
            s3ContentDao.initialize();
        }

        @Override
        protected void shutDown() throws Exception {
        }

    }

}
