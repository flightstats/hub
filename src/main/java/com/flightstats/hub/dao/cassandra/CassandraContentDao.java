package com.flightstats.hub.dao.cassandra;

import com.datastax.driver.core.Session;
import com.flightstats.hub.dao.ContentDao;
import com.flightstats.hub.model.ChannelConfiguration;
import com.flightstats.hub.model.Content;
import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.model.InsertedContentKey;
import com.google.common.base.Optional;
import com.google.inject.Inject;
import org.joda.time.DateTime;

import java.util.Collection;

public class CassandraContentDao implements ContentDao {

    @Inject
    public CassandraContentDao(Session session) {
    }

    @Override
    public InsertedContentKey write(String channelName, Content content, long ttlDays) {
        return null;
    }

    @Override
    public Content read(String channelName, ContentKey key) {
        return null;
    }

    @Override
    public void initializeChannel(ChannelConfiguration configuration) {

    }

    @Override
    public Optional<ContentKey> getKey(String id) {
        return null;
    }

    @Override
    public Collection<ContentKey> getKeys(String channelName, DateTime dateTime) {
        return null;
    }

    @Override
    public void delete(String channelName) {

    }
}
