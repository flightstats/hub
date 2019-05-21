package com.flightstats.hub.webhook;

import com.flightstats.hub.model.ChannelConfig;
import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import javax.inject.Inject;
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
@Slf4j
public class TagWebhook {

    private final WebhookService webhookService;

    @Inject
    public TagWebhook(WebhookService webhookService) {
        this.webhookService = webhookService;
    }

    private Set<Webhook> webhookPrototypesWithTag(String tag) {
        final Set<Webhook> webhookSet = new HashSet<>(webhookService.getAll());

        return webhookSet.stream()
                .filter(wh -> wh.isTagPrototype() && Objects.equals(tag, wh.getTagFromTagUrl()))
                .collect(Collectors.toSet());
    }

    Set<Webhook> allManagedWebhooksForChannel(Set<Webhook> webhookSet, ChannelConfig channelConfig) {
        final String channelName = channelConfig.getName().toLowerCase();
        return webhookSet.stream()
                .filter(wh -> !StringUtils.isEmpty(wh.getChannelUrl())
                        && Objects.equals(channelName, wh.getChannelName().toLowerCase())
                        && wh.isManagedByTag())
                .collect(Collectors.toSet());
    }

    private void ensureChannelHasAssociatedWebhook(Set<Webhook> webhookSet, Webhook wh, ChannelConfig channelConfig) {
        final Set<Webhook> managedWebHooks = allManagedWebhooksForChannel(webhookSet, channelConfig);
        if (managedWebHooks.isEmpty()) {
            Webhook newWHInstance = Webhook.instanceFromTagPrototype(wh, channelConfig);
            log.info("TagWebHook: Adding TagWebhook instance for {} ", channelConfig.getName());
            webhookService.upsert(newWHInstance);
        }
    }

    private void ensureNoOrphans(Set<Webhook> webhookSet, ChannelConfig channelConfig) {
        final Set<Webhook> managedWebHooks = allManagedWebhooksForChannel(webhookSet, channelConfig);
        final Set<String> tags = channelConfig.getTags();
        final Set<Webhook> nonOrphanWebhooks = managedWebHooks.stream()
                .filter(wh -> tags.contains(wh.getManagedByTag()))
                .collect(Collectors.toSet());
        final Sets.SetView<Webhook> orphanedWebhooks = Sets.difference(managedWebHooks, nonOrphanWebhooks);
        for (Webhook orphan : orphanedWebhooks) {
            log.info("Deleting TagWebhook instance for channel " + orphan.getChannelName());
            webhookService.delete(orphan.getName());
        }
    }

    public void updateTagWebhooksDueToChannelConfigChange(ChannelConfig channelConfig) {
        final Set<Webhook> webhookSet = new HashSet<>(webhookService.getAll());

        final Set<String> tags = channelConfig.getTags();
        for (String tag : tags) {
            final Set<Webhook> taggedWebhooks = webhookPrototypesWithTag(tag);
            for (Webhook twh : taggedWebhooks) {
                ensureChannelHasAssociatedWebhook(webhookSet, twh, channelConfig);
            }
        }

        ensureNoOrphans(webhookSet, channelConfig);
    }

    public void deleteAllTagWebhooksForChannel(ChannelConfig channelConfig) {
        final Set<Webhook> webhookSet = new HashSet<>(webhookService.getAll());
        final Set<Webhook> managedWebHooks = allManagedWebhooksForChannel(webhookSet, channelConfig);

        for (Webhook wh : allManagedWebhooksForChannel(managedWebHooks, channelConfig)) {
            webhookService.delete(wh.getName());
        }
    }

}
