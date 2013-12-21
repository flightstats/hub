package com.flightstats.datahub.dao.dynamo;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.flightstats.datahub.dao.*;
import com.google.inject.*;
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
		bind(DynamoConnectorFactory.class).in(Singleton.class);
		bindListener(ChannelMetadataInitialization.buildTypeMatcher(), new ChannelMetadataInitialization());
		bindListener(DataHubValueDaoInitialization.buildTypeMatcher(), new DataHubValueDaoInitialization());
        bind(ChannelService.class).to(SplittingChannelService.class).asEagerSingleton();
        bind(ChannelsCollectionDao.class).to(TimedChannelsCollectionDao.class).in(Singleton.class);
        bind(ChannelsCollectionDao.class)
                .annotatedWith(Names.named(TimedChannelsCollectionDao.DELEGATE))
                .to(CachedChannelsCollectionDao.class);
        bind(ChannelsCollectionDao.class)
                .annotatedWith(Names.named(CachedChannelsCollectionDao.DELEGATE))
                .to(DynamoChannelsCollectionDao.class);

        install(new PrivateModule() {
            @Override
            protected void configure() {

                bind(ChannelDao.class).annotatedWith(Sequential.class).to(ChannelDaoImpl.class).in(Singleton.class);
                expose(ChannelDao.class).annotatedWith(Sequential.class);

                bind(DataHubValueDao.class).to(TimedDataHubValueDao.class).in(Singleton.class);
                bind(DataHubValueDao.class)
                        .annotatedWith(Names.named(TimedDataHubValueDao.DELEGATE))
                        .to(DynamoSequentialDataHubValueDao.class);
            }
        });

        install(new PrivateModule() {
            @Override
            protected void configure() {

                bind(ChannelDao.class).annotatedWith(TimeSeries.class).to(ChannelDaoImpl.class).in(Singleton.class);
                expose(ChannelDao.class).annotatedWith(TimeSeries.class);

                bind(DataHubValueDao.class).to(TimedDataHubValueDao.class).in(Singleton.class);
                bind(DataHubValueDao.class)
                        .annotatedWith(Names.named(TimedDataHubValueDao.DELEGATE))
                        .to(DynamoTimeSeriesDataHubValueDao.class);
            }
        });

        bind(DynamoUtils.class).in(Singleton.class);
	}

    @Inject
    @Provides
    @Singleton
    public AmazonDynamoDBClient buildClient(DynamoConnectorFactory factory) {
        return factory.getClient();
    }
}
