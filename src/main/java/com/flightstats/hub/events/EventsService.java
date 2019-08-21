package com.flightstats.hub.events;

import com.diffplug.common.base.Errors;
import com.flightstats.hub.config.properties.AppProperties;
import com.flightstats.hub.config.properties.LocalHostProperties;
import com.flightstats.hub.dao.ChannelService;
import com.flightstats.hub.dao.ItemRequest;
import com.flightstats.hub.model.ChannelContentKey;
import com.flightstats.hub.model.Content;
import com.flightstats.hub.webhook.WebhookService;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.io.EofException;

import javax.inject.Inject;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

@Singleton
@Slf4j
public class EventsService {
    
    private final ChannelService channelService;
    private final WebhookService webhookService;
    private final AppProperties appProperties;
    private final LocalHostProperties localHostProperties;

    @Inject
    public EventsService(ChannelService channelService,
                         WebhookService webhookService,
                         AppProperties appProperties,
                         LocalHostProperties localHostProperties){
        this.channelService = channelService;
        this.webhookService = webhookService;
        this.appProperties = appProperties;
        this.localHostProperties = localHostProperties;
    }

    private Map<String, EventWebhook> outputStreamMap = new ConcurrentHashMap<>();

    void getAndSendData(String uri, String id) {
        ChannelContentKey key = ChannelContentKey.fromResourcePath(uri);
        ItemRequest itemRequest = ItemRequest.builder()
                .channel(key.getChannel())
                .key(key.getContentKey())
                .build();
        Optional<Content> optional = channelService.get(itemRequest);
        if (optional.isPresent()) {
            Content content = optional.get();
            sendData(id, Errors.rethrow().wrap(contentOutput -> {
                contentOutput.write(content);
                log.debug("sent content {} to {}", id, content.getContentKey());
            }));
        }
    }

    void checkHealth(String id) {
        log.trace("check health {}", id);
        sendData(id, Errors.rethrow().wrap(contentOutput -> {
            contentOutput.writeHeartbeat();
            log.trace("sent heartbeat to {}", id);
        }));
    }

    private void sendData(String id, Consumer<ContentOutput> contentConsumer) {
        try {
            EventWebhook eventWebhook = outputStreamMap.get(id);
            if (eventWebhook == null) {
                log.warn("unable to find id {}", id);
                unregister(id);
            } else {
                contentConsumer.accept(eventWebhook.getContentOutput());
            }
        } catch (Errors.WrappedAsRuntimeException e) {
            if (e.getCause() instanceof EofException) {
                log.error("unable to write, closing " + id);
            } else {
                log.error("unable to send to " + id, e);
            }
            unregister(id);
        } catch (Exception e) {
            log.error("unable to send to " + id, e);
            unregister(id);
        }
    }

    public void register(ContentOutput contentOutput) {
        EventWebhook eventWebhook = new EventWebhook(
                contentOutput,
                webhookService,
                localHostProperties,
                appProperties.getAppUrl(),
                appProperties.getAppEnv());
        log.debug("registering events {}", eventWebhook.getGroupName());
        outputStreamMap.put(eventWebhook.getGroupName(), eventWebhook);
        eventWebhook.start();
    }

    private void unregister(String id) {
        log.debug("unregistering events {}", id);
        EventWebhook remove = outputStreamMap.remove(id);
        if (null != remove) {
            remove.stop();
        } else {
            webhookService.delete(id);
        }
    }

}
