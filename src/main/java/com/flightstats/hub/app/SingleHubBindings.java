package com.flightstats.hub.app;

import com.flightstats.hub.cluster.WatchManager;
import com.flightstats.hub.dao.*;
import com.flightstats.hub.dao.file.FileChannelConfigurationDao;
import com.flightstats.hub.dao.file.FileDocumentationDao;
import com.flightstats.hub.dao.file.FileWebhookDao;
import com.flightstats.hub.dao.file.SingleContentService;
import com.flightstats.hub.model.ChannelConfig;
import com.flightstats.hub.spoke.ChannelTtlEnforcer;
import com.flightstats.hub.spoke.FileSpokeStore;
import com.flightstats.hub.spoke.SpokeReadContentDao;
import com.flightstats.hub.spoke.SpokeStore;
import com.flightstats.hub.spoke.SpokeWriteContentDao;
import com.flightstats.hub.webhook.Webhook;
import com.flightstats.hub.metrics.JVMMetricsService;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.google.inject.name.Names;

class SingleHubBindings extends AbstractModule {

    @Override
    protected void configure() {
        bind(ContentService.class).to(SingleContentService.class).asEagerSingleton();
        bind(DocumentationDao.class).to(FileDocumentationDao.class).asEagerSingleton();
        bind(ChannelTtlEnforcer.class).asEagerSingleton();
        bind(JVMMetricsService.class).asEagerSingleton();
    }

    @Inject
    @Singleton
    @Provides
    @Named("ChannelConfig")
    public static Dao<ChannelConfig> buildChannelConfigDao(WatchManager watchManager, FileChannelConfigurationDao dao) {
        return new CachedLowerCaseDao<>(dao, watchManager, "/channels/cache");
    }

    @Inject
    @Singleton
    @Provides
    @Named("Webhook")
    public static Dao<Webhook> buildWebhookDao(WatchManager watchManager, FileWebhookDao dao) {
        return new CachedDao<>(dao, watchManager, "/webhooks/cache");
    }
}
