package com.flightstats.hub.dao.aws;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.s3.AmazonS3;
import com.flightstats.hub.dao.*;
import com.flightstats.hub.dao.dynamo.DynamoChannelConfigurationDao;
import com.flightstats.hub.dao.dynamo.DynamoContentDao;
import com.flightstats.hub.dao.dynamo.DynamoUtils;
import com.flightstats.hub.dao.s3.S3ContentDao;
import com.flightstats.hub.dao.timeIndex.TimeIndexDao;
import com.flightstats.hub.replication.DynamoReplicationDao;
import com.flightstats.hub.replication.ReplicationInitialization;
import com.flightstats.hub.util.ContentKeyGenerator;
import com.flightstats.hub.util.CuratorKeyGenerator;
import com.flightstats.hub.util.TimeSeriesKeyGenerator;
import com.google.inject.*;
import com.google.inject.name.Names;

import java.io.IOException;
import java.util.Properties;

public class AwsDataStoreModule extends AbstractModule {

	private final Properties properties;

	public AwsDataStoreModule(Properties properties) {
		this.properties = properties;
	}

	@Override
	protected void configure() {
		Names.bindProperties(binder(), properties);
		bind(AwsConnectorFactory.class).in(Singleton.class);
		bindListener(ChannelConfigurationInitialization.buildTypeMatcher(), new ChannelConfigurationInitialization());
		bindListener(ContentDaoInitialization.buildTypeMatcher(), new ContentDaoInitialization());
        bindListener(ReplicationInitialization.buildTypeMatcher(), new ReplicationInitialization());
        bind(DynamoReplicationDao.class).asEagerSingleton();
        bind(ChannelService.class).to(ChannelServiceImpl.class).asEagerSingleton();
        bind(ContentServiceFinder.class).to(SplittingContentServiceFinder.class).asEagerSingleton();
        bind(ChannelConfigurationDao.class).to(TimedChannelConfigurationDao.class).in(Singleton.class);
        bind(ChannelConfigurationDao.class)
                .annotatedWith(Names.named(TimedChannelConfigurationDao.DELEGATE))
                .to(CachedChannelConfigurationDao.class);
        bind(ChannelConfigurationDao.class)
                .annotatedWith(Names.named(CachedChannelConfigurationDao.DELEGATE))
                .to(DynamoChannelConfigurationDao.class);

        install(new PrivateModule() {
            @Override
            protected void configure() {

                bind(ContentService.class).annotatedWith(Sequential.class).to(ContentServiceImpl.class).in(Singleton.class);
                expose(ContentService.class).annotatedWith(Sequential.class);

                bind(ContentDao.class).to(TimedContentDao.class).in(Singleton.class);
                bind(ContentDao.class)
                        .annotatedWith(Names.named(TimedContentDao.DELEGATE))
                        .to(S3ContentDao.class);
                bind(TimeIndexDao.class).to(S3ContentDao.class).in(Singleton.class);
                expose(TimeIndexDao.class);
                bind(KeyCoordination.class).to(SequenceKeyCoordination.class).in(Singleton.class);
                bind(ContentKeyGenerator.class).to(CuratorKeyGenerator.class).in(Singleton.class);
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
                bind(ContentKeyGenerator.class).to(TimeSeriesKeyGenerator.class).in(Singleton.class);
            }
        });

        bind(DynamoUtils.class).in(Singleton.class);
	}

    @Inject
    @Provides
    @Singleton
    public AmazonDynamoDBClient buildDynamoClient(AwsConnectorFactory factory) throws IOException {
        return factory.getDynamoClient();
    }

    @Inject
    @Provides
    @Singleton
    public AmazonS3 buildS3Client(AwsConnectorFactory factory) throws IOException {
        return factory.getS3Client();
    }
}
