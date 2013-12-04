package com.flightstats.datahub.app.config;

import com.datastax.driver.core.Session;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flightstats.datahub.dao.CassandraChannelDao;
import com.flightstats.datahub.dao.CassandraConnectorFactory;
import com.flightstats.datahub.dao.ChannelDao;
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
		bind(CassandraChannelDao.class).asEagerSingleton();
		bind(CassandraConnectorFactory.class).in(Singleton.class);
		bindListener(CassandraChannelMetadataInitialization.buildTypeMatcher(), new CassandraChannelMetadataInitialization());
		bindListener(CqlValueOperationsInitialization.buildTypeMatcher(), new CqlValueOperationsInitialization());
		bind(ChannelDao.class).to(CassandraChannelDao.class).in(Singleton.class);
	}

    @Inject
    @Provides
    @Singleton
    public Session buildSession(CassandraConnectorFactory factory) {
        return factory.getSession();
    }
}
