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
import com.flightstats.hub.metrics.PeriodicMetricEmitter;
import com.flightstats.hub.metrics.PeriodicSpokeMetricEmitter;
import com.flightstats.hub.model.ChannelConfig;
import com.flightstats.hub.spoke.LocalStorageTTLEnforcerForSingleHub;
import com.flightstats.hub.webhook.Webhook;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import java.util.Arrays;
import java.util.Collection;

class SingleHubBindings extends AbstractModule {

    @Override
    protected void configure() {
        bind(ContentService.class).to(SingleContentService.class).asEagerSingleton();  // Channel and channel content managed on file-system.
        bind(DocumentationDao.class).to(FileDocumentationDao.class).asEagerSingleton();
        bind(LocalStorageTTLEnforcerForSingleHub.class).asEagerSingleton();  // This is in place of SpokeTtlEnforcer for WRITE path only. Should be killed for normal spoke enforcement.
    }

    @Provides
    @Inject
    @Singleton
    @Named("PeriodicMetricEmitters")
    public Collection<PeriodicMetricEmitter> buildPeriodicMetricEmitters(PeriodicSpokeMetricEmitter spokeEmitter) {
        return Arrays.asList(spokeEmitter);
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
