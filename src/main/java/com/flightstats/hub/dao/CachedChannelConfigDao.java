package com.flightstats.hub.dao;

import com.flightstats.hub.app.HubServices;
import com.flightstats.hub.cluster.WatchManager;
import com.flightstats.hub.cluster.Watcher;
import com.flightstats.hub.model.ChannelConfig;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.apache.curator.framework.api.CuratorEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CachedChannelConfigDao implements ChannelConfigDao {

    private final static Logger logger = LoggerFactory.getLogger(CachedChannelConfigDao.class);

    public static final String DELEGATE = "CachedChannelMetadataDao.DELEGATE";
    public static final String WATCHER_PATH = "/channels/cache";
    private final ChannelConfigDao delegate;
    private WatchManager watchManager;
    private ConcurrentMap<String, ChannelConfig> channelConfigMap;
    private ExecutorService executor = Executors.newSingleThreadExecutor();

    @Inject
    public CachedChannelConfigDao(@Named(DELEGATE) ChannelConfigDao delegate, WatchManager watchManager) {
        this.delegate = delegate;
        this.watchManager = watchManager;
        this.channelConfigMap = new ConcurrentHashMap<>();
        HubServices.register(new CachedChannelConfigDaoService());
    }

    @Override
    public ChannelConfig createChannel(ChannelConfig config) {
        ChannelConfig channelConfig = delegate.createChannel(config);
        channelConfigMap.put(channelConfig.getName(), channelConfig);
        notifyWatchers();
        return channelConfig;
    }

    @Override
    public void updateChannel(ChannelConfig newConfig) {
        delegate.updateChannel(newConfig);
        channelConfigMap.put(newConfig.getName(), newConfig);
        notifyWatchers();
    }

    @Override
    public boolean channelExists(String name) {
        return getChannelConfig(name) != null;
    }

    @Override
    public ChannelConfig getChannelConfig(String name) {
        /**
         * It is very important to use caching here, as getChannelConfig is called for every read and write.
         */
        ChannelConfig configuration = channelConfigMap.get(name);
        if (configuration != null) {
            return configuration;
        }
        configuration = delegate.getChannelConfig(name);
        if (null != configuration) {
            channelConfigMap.put(name, configuration);
        }
        return configuration;
    }

    @Override
    public Iterable<ChannelConfig> getChannels() {
        return delegate.getChannels();
    }

    private void updateMap() {
        Iterable<ChannelConfig> channels = delegate.getChannels();
        ConcurrentMap<String, ChannelConfig> newMap = new ConcurrentHashMap<>();
        for (ChannelConfig channel : channels) {
            newMap.put(channel.getName(), channel);
        }
        channelConfigMap = newMap;
    }

    @Override
    public void delete(String name) {
        delegate.delete(name);
        channelConfigMap.remove(name);
        notifyWatchers();
    }

    void notifyWatchers() {
        watchManager.notifyWatcher(WATCHER_PATH);
    }

    void startWatcher() {
        CachedChannelConfigDao dao = this;
        watchManager.register(new Watcher() {
            @Override
            public void callback(CuratorEvent event) {
                executor.submit(dao::updateMap);
            }

            @Override
            public String getPath() {
                return WATCHER_PATH;
            }
        });
    }

    private class CachedChannelConfigDaoService extends AbstractIdleService {

        @Override
        protected void startUp() throws Exception {
            startWatcher();
        }

        @Override
        protected void shutDown() throws Exception {
            //do nothing
        }
    }
}
