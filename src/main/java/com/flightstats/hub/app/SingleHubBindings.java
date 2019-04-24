package com.flightstats.hub.app;

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
import com.flightstats.hub.webhook.Webhook;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.google.inject.name.Names;

import java.util.Arrays;
import java.util.Collection;

class SingleHubBindings extends AbstractModule {

    @Override
    protected void configure() {
        bind(ContentService.class).to(SingleContentService.class).asEagerSingleton();  // Channel and channel content managed on file-system.  Merge with ClusterContentService.
        bind(DocumentationDao.class).to(FileDocumentationDao.class).asEagerSingleton();  // Doc management on filesystem instead of S3.

        bind(Dao.class)
                .annotatedWith(Names.named("ChannelConfigDao"))
                .to(FileChannelConfigurationDao.class).asEagerSingleton();
        bind(Dao.class)
                .annotatedWith(Names.named("WebhookDao"))
                .to(FileWebhookDao.class).asEagerSingleton();
    }

    @Provides
    @Inject
    @Singleton
    @Named("ChannelConfigDao")
    public Dao<ChannelConfig> buildChannelConfigDao(FileChannelConfigurationDao dao) {
        return dao;
    }

    @Provides
    @Inject
    @Singleton
    @Named("WebhookDao")
    public Dao<Webhook> buildWebhookDao(FileWebhookDao dao) {
        return dao;
    }

    @Provides
    @Inject
    @Singleton
    @Named("PeriodicMetricEmitters")
    public Collection<PeriodicMetricEmitter> buildPeriodicMetricEmitters(PeriodicSpokeMetricEmitter spokeEmitter) {
        return Arrays.asList(spokeEmitter);
    }
}
