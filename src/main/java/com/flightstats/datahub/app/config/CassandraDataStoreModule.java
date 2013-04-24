package com.flightstats.datahub.app.config;

import com.flightstats.datahub.dao.CassandraChannelDao;
import com.flightstats.datahub.dao.CassandraConnector;
import com.flightstats.datahub.dao.CassandraConnectorFactory;
import com.flightstats.datahub.dao.ChannelDao;
import com.flightstats.datahub.model.ChannelConfiguration;
import com.flightstats.datahub.model.serialize.JacksonHectorSerializer;
import com.google.inject.*;
import com.google.inject.name.Names;
import me.prettyprint.hector.api.Serializer;
import org.codehaus.jackson.map.ObjectMapper;

import java.util.Properties;

class CassandraDataStoreModule extends AbstractModule {

	private final ObjectMapper objectMapper = new DataHubObjectMapperFactory().build();
	private final JacksonHectorSerializer<ChannelConfiguration> jacksonHectorSerializer = new JacksonHectorSerializer<>(objectMapper, ChannelConfiguration.class);
	private final Properties properties;

	public CassandraDataStoreModule(Properties properties) {
		this.properties = properties;
	}

	@Override
	protected void configure() {
		Names.bindProperties(binder(), properties);
		bind(CassandraChannelDao.class).asEagerSingleton();
		bind(CassandraConnectorFactory.class).in(Singleton.class);
		bind(new TypeLiteral<Serializer<ChannelConfiguration>>() {
		}).toInstance(jacksonHectorSerializer);
		bind(ChannelDao.class).to(CassandraChannelDao.class).in(Singleton.class);
	}

	@Inject
	@Provides
	@Singleton
	public CassandraConnector buildCassandraConnector(CassandraConnectorFactory factory) {
		return factory.build();
	}

}
