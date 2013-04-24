package com.flightstats.datahub.app.config;

import com.flightstats.datahub.dao.ChannelDao;
import com.flightstats.datahub.dao.prototypes.InMemoryChannelDao;
import com.google.inject.Singleton;

class MemoryBackedDataHubModule extends BaseDataHubModule {

	@Override
	protected void configureServlets() {
		bind(ChannelDao.class).to(InMemoryChannelDao.class).in(Singleton.class);
		super.configureBaseServlets();
	}

}
