package com.flightstats.hub.app;

import com.flightstats.hub.dao.*;
import com.flightstats.hub.dao.encryption.AuditChannelService;
import com.flightstats.hub.dao.encryption.BasicChannelService;
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
        if (Boolean.parseBoolean(HubProperties.getProperty("app.encrypted", "false"))) {
            logger.info("using encrypted hub");
            bind(ChannelService.class).annotatedWith(BasicChannelService.class).to(NasChannelService.class).asEagerSingleton();
            bind(ChannelService.class).to(AuditChannelService.class).asEagerSingleton();
        } else {
            logger.info("using normal hub");
            bind(ChannelService.class).to(NasChannelService.class).asEagerSingleton();
        }

        bind(ChannelConfigurationDao.class).to(CachedChannelConfigurationDao.class).asEagerSingleton();
        bind(ChannelConfigurationDao.class)
                .annotatedWith(Names.named(CachedChannelConfigurationDao.DELEGATE))
                .to(NasChannelConfigurationDao.class);

        bind(ContentService.class).to(NasContentService.class).asEagerSingleton();
        bind(FileSpokeStore.class).asEagerSingleton();

        bind(GroupDao.class).to(NasGroupDao.class).asEagerSingleton();
    }

}
