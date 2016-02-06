package com.flightstats.hub.app;

import com.flightstats.hub.dao.CachedChannelConfigDao;
import com.flightstats.hub.dao.ChannelConfigDao;
import com.flightstats.hub.dao.ContentService;
import com.flightstats.hub.dao.nas.NasChannelConfigurationDao;
import com.flightstats.hub.dao.nas.NasContentService;
import com.flightstats.hub.dao.nas.NasGroupDao;
import com.flightstats.hub.dao.nas.NasTtlEnforcer;
import com.flightstats.hub.group.GroupDao;
import com.google.inject.AbstractModule;
import com.google.inject.name.Names;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NasBindings extends AbstractModule {
    private final static Logger logger = LoggerFactory.getLogger(NasBindings.class);

    @Override
    protected void configure() {
        bind(ChannelConfigDao.class).to(CachedChannelConfigDao.class).asEagerSingleton();
        bind(ChannelConfigDao.class)
                .annotatedWith(Names.named(CachedChannelConfigDao.DELEGATE))
                .to(NasChannelConfigurationDao.class);

        bind(ContentService.class).to(NasContentService.class).asEagerSingleton();
        bind(GroupDao.class).to(NasGroupDao.class).asEagerSingleton();
        bind(NasTtlEnforcer.class).asEagerSingleton();
        bind(FinalCheck.class).to(PassFinalCheck.class).asEagerSingleton();
    }

    public static String packages() {
        return "com.flightstats.hub.alert," +
                "com.flightstats.hub.app," +
                "com.flightstats.hub.channel," +
                "com.flightstats.hub.exception," +
                "com.flightstats.hub.filter," +
                "com.flightstats.hub.group," +
                "com.flightstats.hub.health," +
                "com.flightstats.hub.metrics," +
                "com.flightstats.hub.replication," +
                "com.flightstats.hub.events," +
                "com.flightstats.hub.ws";
    }

}
