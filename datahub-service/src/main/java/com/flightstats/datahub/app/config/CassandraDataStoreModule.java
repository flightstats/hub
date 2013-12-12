package com.flightstats.datahub.app.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flightstats.datahub.dao.*;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Names;

import java.util.Properties;

class CassandraDataStoreModule extends AbstractModule {

	private final ObjectMapper objectMapper = DataHubObjectMapperFactory.construct();
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
		bind(ChannelDao.class).to(ChannelDaoImpl.class).in(Singleton.class);
		bind(ChannelsCollectionDao.class).to(CassandraChannelsCollectionDao.class).in(Singleton.class);
		bind(DataHubValueDao.class).to(CassandraDataHubValueDao.class).in(Singleton.class);
	}

    @Inject
    @Provides
    @Singleton
    public QuorumSession buildSession(CassandraConnectorFactory factory) {
        return factory.getSession();
    }
}
