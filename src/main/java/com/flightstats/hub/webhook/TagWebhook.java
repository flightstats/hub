package com.flightstats.hub.webhook;

import com.flightstats.hub.app.HubProvider;
import com.flightstats.hub.dao.ChannelService;
import com.flightstats.hub.dao.Dao;
import com.flightstats.hub.dao.LocalChannelService;
import com.flightstats.hub.model.ChannelConfig;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Sets;
import com.google.inject.TypeLiteral;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * TagWebhook is an automation of webhooks defined on channel Tags.
 * <p>
 * Example: if a webhook is defined on a channel tag that has 3 members,
 * a webhook will be created for each of those channels that have that tag.
 * Furthermore, when other channels get assigned the tag (channel create or update,
 * a webhook will be created for that newly tagged channel.
 * <p>
 * When a tagged channel is deleted, the associated webhook would also be deleted.
 */
public class TagWebhook {
    private final static Logger logger = LoggerFactory.getLogger(WebhookResource.class);
    private final static WebhookService webhookService = HubProvider.getInstance(WebhookService.class);
    private final static ChannelService channelService = HubProvider.getInstance(LocalChannelService.class);
    private final static Dao<Webhook> webhookDao = HubProvider.getInstance(
            new TypeLiteral<Dao<Webhook>>() {
            }, "Webhook");

    protected static Set<Webhook> webhookPrototypesWithTag(String tag) {
        Set<Webhook> webhookSet = new HashSet<>(webhookDao.getAll(false));

        Predicate<Webhook> withTagName = new Predicate<Webhook>() {
            @Override
            public boolean apply(Webhook wh) {
                return tag.equals(wh.getTag());
            }
        };
        Predicate<Webhook> isTagWebhookPrototype = new Predicate<Webhook>() {
            @Override
            public boolean apply(Webhook wh) {
                return wh.isTagPrototype();
            }
        };
        return FluentIterable.from(webhookSet)
                .filter(withTagName)
                .filter(isTagWebhookPrototype)
                .toSet();
    }

    protected static Set<Webhook> allManagedWebhooksForChannel(Set<Webhook> webhookSet, ChannelConfig channelConfig) {
        String channelName = channelConfig.getName();
        Predicate<Webhook> withChannelName = new Predicate<Webhook>() {
            @Override
            public boolean apply(Webhook wh) {
                return channelName.equals(wh.getChannelName());
            }
        };
        Predicate<Webhook> isManagedByTag = new Predicate<Webhook>() {
            @Override
            public boolean apply(Webhook wh) {
                return wh.isManagedByTag();
            }
        };
        return FluentIterable.from(webhookSet)
                .filter(withChannelName)
                .filter(isManagedByTag)
                .toSet();
    }

    protected static void ensureChannelHasAssociatedWebhook(Set<Webhook> webhookSet, Webhook wh, ChannelConfig channelConfig) {
        Set<Webhook> managedWebHooks = allManagedWebhooksForChannel(webhookSet, channelConfig);
        if (managedWebHooks.isEmpty()) {
            Webhook newWHInstance = Webhook.instanceFromTagPrototype(wh, channelConfig);
            logger.info("Adding TagWebhook instance for " + channelConfig.getName());
            webhookService.upsert(newWHInstance);
        }
    }

    protected static void ensureNoOrphans(Set<Webhook> webhookSet, ChannelConfig channelConfig) {
        Set<Webhook> managedWebHooks = allManagedWebhooksForChannel(webhookSet, channelConfig);
        Set<String> tags = channelConfig.getTags();
        Predicate<Webhook> isWebhookTagMemberOfChannelTags = new Predicate<Webhook>() {
            @Override
            public boolean apply(Webhook wh) {
                return tags.contains(wh.getTag());
            }
        };
        Set<Webhook> nonOrphanWebhooks = FluentIterable.from(managedWebHooks)
                .filter(isWebhookTagMemberOfChannelTags)
                .toSet();
        Sets.SetView<Webhook> orphanedWebhooks = Sets.difference(managedWebHooks, nonOrphanWebhooks);
        for (Webhook orphan : orphanedWebhooks) {
            logger.info("Deleting TagWebhook instance for channel " + orphan.getChannelName());
            webhookService.delete(orphan.getName());
        }
    }


    public static void updateTagWebhooks(ChannelConfig channelConfig) {
        Set<Webhook> webhookSet = new HashSet<>(webhookDao.getAll(false));  // should I pass this in?

        Set<String> tags = channelConfig.getTags();
        for (String tag : tags) {
            Set<Webhook> taggedWebhooks = webhookPrototypesWithTag(tag);
            for (Webhook twh : taggedWebhooks) {
                ensureChannelHasAssociatedWebhook(webhookSet, twh, channelConfig);
            }
        }

        ensureNoOrphans(webhookSet, channelConfig);
    }

    public static void deleteAllTagWebhooksForChannel(ChannelConfig channelConfig) {
        Set<Webhook> webhookSet = new HashSet<>(webhookDao.getAll(false));  // should I pass this in?
        Set<Webhook> managedWebHooks = allManagedWebhooksForChannel(webhookSet, channelConfig);

        for (Webhook wh : allManagedWebhooksForChannel(managedWebHooks, channelConfig)) {
            webhookService.delete(wh.getName());
        }
    }

    // Add new wh instances for new or updated tag webhook
    public static void addTagWebhookInstances(Webhook webhook) {
        Collection<ChannelConfig> channels = channelService.getChannels(webhook.getTag(), false);
        for (ChannelConfig channel : channels) {
            logger.info("Adding TagWebhook instance for " + channel.getName());
            webhookService.upsert(Webhook.instanceFromTagPrototype(webhook, channel));
        }
    }
}