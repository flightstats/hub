package com.flightstats.hub.dao.memory;

import com.flightstats.hub.dao.*;
import com.flightstats.hub.dao.timeIndex.TimeIndexDao;
import com.flightstats.hub.replication.MemoryReplicationDao;
import com.flightstats.hub.replication.NoOpReplicationService;
import com.flightstats.hub.replication.ReplicationDao;
import com.flightstats.hub.replication.ReplicationService;
import com.flightstats.hub.websocket.MemoryWebsocketPublisher;
import com.flightstats.hub.websocket.WebsocketPublisher;
import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import com.google.inject.name.Names;

import java.util.Properties;

public class MemoryModule extends AbstractModule {

	private final Properties properties;

	public MemoryModule(Properties properties) {
		this.properties = properties;
	}

	@Override
	protected void configure() {
		Names.bindProperties(binder(), properties);
        bind(ChannelService.class).to(ChannelServiceImpl.class).asEagerSingleton();
        bind(ChannelConfigurationDao.class).to(MemoryChannelConfigurationDao.class).asEagerSingleton();
        bind(ContentServiceFinder.class).to(SingleContentServiceFinder.class).asEagerSingleton();
        bind(ContentService.class).to(ContentServiceImpl.class).asEagerSingleton();
        bind(ContentDao.class).to(MemoryContentDao.class).asEagerSingleton();
        bind(KeyCoordination.class).to(MemoryKeyCoordination.class).asEagerSingleton();
        bind(WebsocketPublisher.class).to(MemoryWebsocketPublisher.class).asEagerSingleton();
        bind(ReplicationService.class).to(NoOpReplicationService.class).asEagerSingleton();
        bind(TimeIndexDao.class).to(MemoryContentDao.class).asEagerSingleton();
        bind(ReplicationDao.class).to(MemoryReplicationDao.class).in(Singleton.class);
	}

}
