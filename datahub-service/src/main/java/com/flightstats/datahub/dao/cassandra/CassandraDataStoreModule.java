package com.flightstats.datahub.dao.cassandra;

import com.flightstats.datahub.dao.*;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Names;

import java.util.Properties;

public class CassandraDataStoreModule extends AbstractModule {

	private final Properties properties;

	public CassandraDataStoreModule(Properties properties) {
		this.properties = properties;
	}

	@Override
	protected void configure() {
		Names.bindProperties(binder(), properties);
		bind(ChannelDaoImpl.class).asEagerSingleton();
		bind(CassandraConnectorFactory.class).in(Singleton.class);
		bindListener(ChannelMetadataInitialization.buildTypeMatcher(), new ChannelMetadataInitialization());
		bindListener(DataHubValueDaoInitialization.buildTypeMatcher(), new DataHubValueDaoInitialization());
		bind(ChannelService.class).to(SimpleChannelService.class).in(Singleton.class);
        bind(ChannelDao.class).to(ChannelDaoImpl.class).in(Singleton.class);

        bind(ChannelsCollectionDao.class).to(TimedChannelsCollectionDao.class).in(Singleton.class);
        bind(ChannelsCollectionDao.class)
                .annotatedWith(Names.named(TimedChannelsCollectionDao.DELEGATE))
                .to(CachedChannelsCollectionDao.class);
        bind(ChannelsCollectionDao.class)
                .annotatedWith(Names.named(CachedChannelsCollectionDao.DELEGATE))
                .to(CassandraChannelsCollectionDao.class);

        bind(DataHubValueDao.class).to(TimedDataHubValueDao.class).in(Singleton.class);
        bind(DataHubValueDao.class)
                .annotatedWith(Names.named(TimedDataHubValueDao.DELEGATE))
                .to(CassandraDataHubValueDao.class);
	}

    @Inject
    @Provides
    @Singleton
    public QuorumSession buildSession(CassandraConnectorFactory factory) {
        return factory.getSession();
    }
}
