package com.flightstats.hub.webhook;

import com.flightstats.hub.dao.ChannelService;
import com.flightstats.hub.dao.Dao;
import com.flightstats.hub.model.ChannelConfig;
import com.google.common.base.Optional;
import com.google.common.collect.Sets;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

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

    @Inject
    private static WebhookService webhookService;

    @Inject
    private static ChannelService channelService;

    @Inject
    private static LocalWebhookManager localWebhookManager;

    @Inject
    @Named("Webhook")
    private static Dao<Webhook> webhookDao;

    private static Set<Webhook> webhookPrototypesWithTag(String tag) {
        Set<Webhook> webhookSet = new HashSet<>(webhookDao.getAll(false));

        return webhookSet.stream()
                .filter(wh -> wh.isTagPrototype() && Objects.equals(tag, wh.getTagFromTagUrl()))
                .collect(Collectors.toSet());
    }

    static Set<Webhook> webhookInstancesWithTag(String tag) {
        Set<Webhook> webhookSet = new HashSet<>(webhookDao.getAll(false));

        return webhookSet.stream()
                .filter(wh -> !wh.isTagPrototype() && Objects.equals(tag, wh.getManagedByTag()))
                .collect(Collectors.toSet());
    }

    static Set<Webhook> allManagedWebhooksForChannel(Set<Webhook> webhookSet, ChannelConfig channelConfig) {
        String channelName = channelConfig.getName().toLowerCase();
        return webhookSet.stream()
                .filter(wh -> !StringUtils.isEmpty(wh.getChannelUrl()) && Objects.equals(channelName, wh.getChannelName().toLowerCase()) && wh.isManagedByTag())
                .collect(Collectors.toSet());
    }

    private static void ensureChannelHasAssociatedWebhook(Set<Webhook> webhookSet, Webhook wh, ChannelConfig channelConfig) {
        Set<Webhook> managedWebHooks = allManagedWebhooksForChannel(webhookSet, channelConfig);
        if (managedWebHooks.isEmpty()) {
            Webhook newWHInstance = webhookService.fromTagPrototype(wh, channelConfig.getName());
            logger.info("TagWebHook: Adding TagWebhook instance for " + channelConfig.getName());
            webhookService.upsert(newWHInstance);
        }
    }

    private static void ensureNoOrphans(Set<Webhook> webhookSet, ChannelConfig channelConfig) {
        Set<Webhook> managedWebHooks = allManagedWebhooksForChannel(webhookSet, channelConfig);
        Set<String> tags = channelConfig.getTags();
        Set<Webhook> nonOrphanWebhooks = managedWebHooks.stream()
                .filter(wh -> tags.contains(wh.getManagedByTag()))
                .collect(Collectors.toSet());
        Sets.SetView<Webhook> orphanedWebhooks = Sets.difference(managedWebHooks, nonOrphanWebhooks);
        for (Webhook orphan : orphanedWebhooks) {
            logger.info("Deleting TagWebhook instance for channel " + orphan.getChannelName());
            webhookService.delete(orphan.getName());
        }
    }


    public static void updateTagWebhooksDueToChannelConfigChange(ChannelConfig channelConfig) {
        Set<Webhook> webhookSet = new HashSet<>(webhookDao.getAll(false));

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
        Set<Webhook> webhookSet = new HashSet<>(webhookDao.getAll(false));
        Set<Webhook> managedWebHooks = allManagedWebhooksForChannel(webhookSet, channelConfig);

        for (Webhook wh : allManagedWebhooksForChannel(managedWebHooks, channelConfig)) {
            webhookService.delete(wh.getName());
        }
    }

    // Add new wh instances for new or updated tag webhook
    static void upsertTagWebhookInstances(Webhook webhookPrototype) {
        Collection<ChannelConfig> channels = channelService.getChannels(webhookPrototype.getTagFromTagUrl(), false);
        for (ChannelConfig channel : channels) {
            logger.info("TagWebHook: Adding TagWebhook instance for " + channel.getName());
            webhookService.upsert(webhookService.fromTagPrototype(webhookPrototype, channel.getName()));
        }
    }

    static void deleteInstancesIfTagWebhook(String webhookName) {
        Optional<Webhook> webhookOptional = webhookService.get(webhookName);
        if (!webhookOptional.isPresent()) return;
        Webhook webhook = webhookOptional.get();
        if (!webhook.isTagPrototype()) return;
        logger.info("TagWebHook: Deleting tag webhook instances for tag " + webhookName);

        Set<String> names = webhookInstancesWithTag(webhook.getTagFromTagUrl()).stream()
                .map((Webhook::getName))
                .collect(Collectors.toSet());

        localWebhookManager.runAndWait("TagWebhook.deleteAll", names, webhookService::delete);
    }
}
