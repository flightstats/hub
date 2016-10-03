package com.flightstats.hub.app;

import com.flightstats.hub.cluster.WatchManager;
import com.flightstats.hub.dao.*;
import com.flightstats.hub.dao.file.FileContentService;
import com.flightstats.hub.dao.file.NasChannelConfigurationDao;
import com.flightstats.hub.dao.file.NasTtlEnforcer;
import com.flightstats.hub.dao.file.NasWebhookDao;
import com.flightstats.hub.model.ChannelConfig;
import com.flightstats.hub.webhook.Webhook;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

class FileBindings extends AbstractModule {

    static String packages() {
        return "com.flightstats.hub.alert," +
                "com.flightstats.hub.app," +
                "com.flightstats.hub.channel," +
                "com.flightstats.hub.events," +
                "com.flightstats.hub.exception," +
                "com.flightstats.hub.filter," +
                "com.flightstats.hub.health," +
                "com.flightstats.hub.metrics," +
                "com.flightstats.hub.replication," +
                "com.flightstats.hub.time," +
                "com.flightstats.hub.webhook," +
                "com.flightstats.hub.ws";
    }

    @Override
    protected void configure() {
        bind(ChannelService.class).to(LocalChannelService.class).asEagerSingleton();
        bind(ContentService.class).to(FileContentService.class).asEagerSingleton();
        bind(NasTtlEnforcer.class).asEagerSingleton();
        bind(FinalCheck.class).to(PassFinalCheck.class).asEagerSingleton();
    }

    @Inject
    @Singleton
    @Provides
    @Named("ChannelConfig")
    public static Dao<ChannelConfig> buildChannelConfigDao(WatchManager watchManager, NasChannelConfigurationDao dao) {
        return new CachedDao<>(dao, watchManager, "/channels/cache");
    }

    @Inject
    @Singleton
    @Provides
    @Named("Webhook")
    public static Dao<Webhook> buildWebhookDao(WatchManager watchManager, NasWebhookDao dao) {
        return new CachedDao<>(dao, watchManager, "/webhooks/cache");
    }
}
