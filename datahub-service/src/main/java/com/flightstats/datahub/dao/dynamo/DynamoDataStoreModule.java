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
        bind(ChannelMetadataDao.class).to(TimedChannelMetadataDao.class).in(Singleton.class);
        bind(ChannelMetadataDao.class)
                .annotatedWith(Names.named(TimedChannelMetadataDao.DELEGATE))
                .to(CachedChannelMetadataDao.class);
        bind(ChannelMetadataDao.class)
                .annotatedWith(Names.named(CachedChannelMetadataDao.DELEGATE))
                .to(DynamoChannelMetadataDao.class);

        install(new PrivateModule() {
            @Override
            protected void configure() {

                bind(ContentService.class).annotatedWith(Sequential.class).to(ContentServiceImpl.class).in(Singleton.class);
                expose(ContentService.class).annotatedWith(Sequential.class);

                bind(ContentDao.class).to(TimedContentDao.class).in(Singleton.class);
                bind(ContentDao.class)
                        .annotatedWith(Names.named(TimedContentDao.DELEGATE))
                        .to(DynamoSequentialContentDao.class);
            }
        });

        install(new PrivateModule() {
            @Override
            protected void configure() {

                bind(ContentService.class).annotatedWith(TimeSeries.class).to(ContentServiceImpl.class).in(Singleton.class);
                expose(ContentService.class).annotatedWith(TimeSeries.class);

                bind(ContentDao.class).to(TimedContentDao.class).in(Singleton.class);
                bind(ContentDao.class)
                        .annotatedWith(Names.named(TimedContentDao.DELEGATE))
                        .to(DynamoTimeSeriesContentDao.class);
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
