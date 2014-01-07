package com.flightstats.datahub.dao.dynamo;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.s3.AmazonS3;
import com.flightstats.datahub.dao.*;
import com.flightstats.datahub.dao.s3.S3ContentDao;
import com.flightstats.datahub.util.CuratorKeyGenerator;
import com.flightstats.datahub.util.DataHubKeyGenerator;
import com.flightstats.datahub.util.TimeSeriesKeyGenerator;
import com.google.inject.*;
import com.google.inject.name.Names;

import java.io.IOException;
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
        bind(ChannelService.class).to(ChannelServiceImpl.class).asEagerSingleton();
        bind(ContentServiceFinder.class).to(SplittingContentServiceFinder.class).asEagerSingleton();
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
                        .to(S3ContentDao.class);
                bind(KeyCoordination.class).to(SequenceKeyCoordination.class).in(Singleton.class);
                bind(DataHubKeyGenerator.class).to(CuratorKeyGenerator.class).in(Singleton.class);
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
                        .to(DynamoContentDao.class);
                bind(KeyCoordination.class).to(TimeSeriesKeyCoordination.class).in(Singleton.class);
                bind(DataHubKeyGenerator.class).to(TimeSeriesKeyGenerator.class).in(Singleton.class);
            }
        });

        bind(DynamoUtils.class).in(Singleton.class);
	}

    @Inject
    @Provides
    @Singleton
    public AmazonDynamoDBClient buildDynamoClient(DynamoConnectorFactory factory) {
        return factory.getDynamoClient();
    }

    @Inject
    @Provides
    @Singleton
    public AmazonS3 buildS3Client(DynamoConnectorFactory factory) throws IOException {
        return factory.getS3Client();
    }
}
