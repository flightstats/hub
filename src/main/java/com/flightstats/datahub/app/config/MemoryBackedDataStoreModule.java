package com.flightstats.datahub.app.config;

import com.flightstats.datahub.dao.ChannelDao;
import com.flightstats.datahub.dao.prototypes.InMemoryChannelDao;
import com.google.inject.AbstractModule;
import com.google.inject.Singleton;

class MemoryBackedDataStoreModule extends AbstractModule {

	@Override
	protected void configure() {
		bind(ChannelDao.class).to(InMemoryChannelDao.class).in(Singleton.class);
	}
}
