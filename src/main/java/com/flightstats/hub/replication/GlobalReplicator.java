package com.flightstats.hub.replication;

import com.flightstats.hub.app.HubProvider;
import com.flightstats.hub.model.ChannelConfig;
import com.flightstats.hub.util.HubUtils;
import com.flightstats.hub.webhook.Webhook;
import com.flightstats.hub.webhook.WebhookService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class GlobalReplicator implements Replicator {

    private final static Logger logger = LoggerFactory.getLogger(GlobalReplicator.class);

    private static final HubUtils hubUtils = HubProvider.getInstance(HubUtils.class);
    private static final WebhookService webhookService = HubProvider.getInstance(WebhookService.class);

    private final String satellite;
    private final ChannelConfig channel;

    GlobalReplicator(ChannelConfig channel, String satellite) {
        this.channel = channel;
        this.satellite = satellite;
    }

    public void start() {
        String channelName = channel.getDisplayName();
        try {
            logger.info("starting global replication {}", channel);
            hubUtils.putChannel(satellite + "internal/global/satellite/" + channelName, channel);
            String groupName = getGroupName();
            logger.info("put channel {} {}", channel, groupName);
            Webhook webhook = Webhook.builder()
                    .name(groupName)
                    .callbackUrl(satellite + "internal/global/repl/" + channelName)
                    .channelUrl(channel.getGlobal().getMaster() + "channel/" + channelName)
                    .heartbeat(true)
                    .batch(Webhook.SECOND)
                    .build();
            webhookService.upsert(webhook);
            logger.info("upserted group {} {}", channel, webhook);
        } catch (Exception e) {
            logger.warn("unable to start " + channelName + " " + getGroupName(), e);
        }
    }

    private String getGroupName() {
        return "Global_" + getKey();
    }

    public ChannelConfig getChannel() {
        return channel;
    }

    public void stop() {
        webhookService.delete(getGroupName());
    }

    String getKey() {
        String domain = StringUtils.removeEnd(StringUtils.substringAfter(satellite, "://"), "/");
        return StringUtils.replace(StringUtils.replace(domain, ":", "_"), ".", "_") + "_" + channel.getDisplayName();
    }

}
