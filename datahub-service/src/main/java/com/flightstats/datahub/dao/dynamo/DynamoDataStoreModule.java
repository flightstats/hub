package com.flightstats.datahub.dao.dynamo;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.flightstats.datahub.dao.*;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Names;

import java.util.Properties;

public class DynamoDataStoreModule extends AbstractModule {

	private final Properties properties;

	public DynamoDataStoreModule(Properties properties) {
		this.properties = properties;
	}

	@Override
	protected void configure() {
		Names.bindProperties(binder(), properties);
		bind(ChannelDaoImpl.class).asEagerSingleton();
		bind(DynamoConnectorFactory.class).in(Singleton.class);
		bindListener(ChannelMetadataInitialization.buildTypeMatcher(), new ChannelMetadataInitialization());
		bindListener(DataHubValueDaoInitialization.buildTypeMatcher(), new DataHubValueDaoInitialization());
		bind(ChannelDao.class).to(ChannelDaoImpl.class).in(Singleton.class);
		bind(ChannelsCollectionDao.class).to(CachedChannelsCollectionDao.class).in(Singleton.class);
        bind(ChannelsCollectionDao.class)
                .annotatedWith(Names.named(CachedChannelsCollectionDao.DELEGATE))
                .to(DynamoChannelsCollectionDao.class);
		bind(DataHubValueDao.class).to(DynamoDataHubValueDao.class).in(Singleton.class);
        bind(DynamoUtils.class).in(Singleton.class);
	}

    @Inject
    @Provides
    @Singleton
    public AmazonDynamoDBClient buildClient(DynamoConnectorFactory factory) {
        return factory.getClient();
    }
}
