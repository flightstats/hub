package com.flightstats.hub.app;

import com.flightstats.hub.cluster.WatchManager;
import com.flightstats.hub.dao.CachedDao;
import com.flightstats.hub.dao.CachedLowerCaseDao;
import com.flightstats.hub.dao.ContentService;
import com.flightstats.hub.dao.Dao;
import com.flightstats.hub.dao.DocumentationDao;
import com.flightstats.hub.dao.file.FileChannelConfigurationDao;
import com.flightstats.hub.dao.file.FileDocumentationDao;
import com.flightstats.hub.dao.file.FileWebhookDao;
import com.flightstats.hub.dao.file.SingleContentService;
import com.flightstats.hub.model.ChannelConfig;
import com.flightstats.hub.spoke.ChannelTtlEnforcer;
import com.flightstats.hub.webhook.Webhook;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import lombok.extern.slf4j.Slf4j;

@Slf4j
class StandaloneModule extends AbstractModule {

    @Override
    protected void configure() {
        log.info("configuring StandaloneModule");
        bind(ContentService.class).to(SingleContentService.class).asEagerSingleton();
        bind(DocumentationDao.class).to(FileDocumentationDao.class).asEagerSingleton();
        bind(ChannelTtlEnforcer.class).asEagerSingleton();
    }

    @Provides
    @Singleton
    @Named("ChannelConfig")
    public static Dao<ChannelConfig> buildChannelConfigDao(WatchManager watchManager, FileChannelConfigurationDao dao) {
        log.info("provides 'ChannelConfig' Dao<ChannelConfig>");
        return new CachedLowerCaseDao<>(dao, watchManager, "/channels/cache");
    }

    @Provides
    @Singleton
    @Named("Webhook")
    public static Dao<Webhook> buildWebhookDao(WatchManager watchManager, FileWebhookDao dao) {
        log.info("provides 'Webhook' Dao<Webhook>");
        return new CachedDao<>(dao, watchManager, "/webhooks/cache");
    }

}
