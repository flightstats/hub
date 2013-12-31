package com.flightstats.datahub.dao.cassandra;

import com.flightstats.datahub.dao.*;
import com.flightstats.datahub.util.CuratorKeyGenerator;
import com.flightstats.datahub.util.DataHubKeyGenerator;
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
		bind(ContentServiceImpl.class).asEagerSingleton();
		bind(CassandraConnectorFactory.class).in(Singleton.class);
		bindListener(ChannelMetadataInitialization.buildTypeMatcher(), new ChannelMetadataInitialization());
		bindListener(DataHubValueDaoInitialization.buildTypeMatcher(), new DataHubValueDaoInitialization());
		bind(ChannelService.class).to(ChannelServiceImpl.class).in(Singleton.class);
		bind(ContentServiceFinder.class).to(SingleContentServiceFinder.class).in(Singleton.class);
        bind(ContentService.class).to(ContentServiceImpl.class).in(Singleton.class);
        bind(KeyCoordination.class).to(SequenceKeyCoordination.class).in(Singleton.class);

        bind(ChannelMetadataDao.class).to(TimedChannelMetadataDao.class).in(Singleton.class);
        bind(ChannelMetadataDao.class)
                .annotatedWith(Names.named(TimedChannelMetadataDao.DELEGATE))
                .to(CachedChannelMetadataDao.class);
        bind(ChannelMetadataDao.class)
                .annotatedWith(Names.named(CachedChannelMetadataDao.DELEGATE))
                .to(CassandraChannelMetadataDao.class);

        bind(ContentDao.class).to(TimedContentDao.class).in(Singleton.class);
        bind(ContentDao.class)
                .annotatedWith(Names.named(TimedContentDao.DELEGATE))
                .to(CassandraContentDao.class);
        bind(DataHubKeyGenerator.class).to(CuratorKeyGenerator.class).in(Singleton.class);
	}

    @Inject
    @Provides
    @Singleton
    public QuorumSession buildSession(CassandraConnectorFactory factory) {
        return factory.getSession();
    }
}
