package com.flightstats.hub.app;

import com.flightstats.hub.dao.CachedChannelConfigurationDao;
import com.flightstats.hub.dao.ChannelConfigurationDao;
import com.flightstats.hub.dao.ContentService;
import com.flightstats.hub.dao.nas.NasChannelConfigurationDao;
import com.flightstats.hub.dao.nas.NasContentService;
import com.flightstats.hub.group.GroupDao;
import com.flightstats.hub.group.NasGroupDao;
import com.flightstats.hub.spoke.FileSpokeStore;
import com.google.inject.AbstractModule;
import com.google.inject.name.Names;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NasBindings extends AbstractModule {
    private final static Logger logger = LoggerFactory.getLogger(NasBindings.class);

    @Override
    protected void configure() {
        bind(ChannelConfigurationDao.class).to(CachedChannelConfigurationDao.class).asEagerSingleton();
        bind(ChannelConfigurationDao.class)
                .annotatedWith(Names.named(CachedChannelConfigurationDao.DELEGATE))
                .to(NasChannelConfigurationDao.class);

        bind(ContentService.class).to(NasContentService.class).asEagerSingleton();
        bind(FileSpokeStore.class).asEagerSingleton();

        bind(GroupDao.class).to(NasGroupDao.class).asEagerSingleton();
    }

}
