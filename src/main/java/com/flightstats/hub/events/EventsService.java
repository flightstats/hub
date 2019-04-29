package com.flightstats.hub.events;

import com.diffplug.common.base.Errors;
import com.flightstats.hub.dao.ChannelService;
import com.flightstats.hub.dao.ItemRequest;
import com.flightstats.hub.model.ChannelContentKey;
import com.flightstats.hub.model.Content;
import com.flightstats.hub.webhook.WebhookService;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.eclipse.jetty.io.EofException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

@Singleton
public class EventsService {

    private final static Logger logger = LoggerFactory.getLogger(EventsService.class);

    @Inject
    private ChannelService channelService;
    @Inject
    private WebhookService webhookService;

    private Map<String, EventWebhook> outputStreamMap = new ConcurrentHashMap<>();

    void getAndSendData(String uri, String id) {
        logger.trace("got uri {} {}", uri, id);
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
                logger.trace("sent content {} to {}", id, content.getContentKey());
            }));
        }
    }

    void checkHealth(String id) {
        logger.trace("check health {}", id);
        sendData(id, Errors.rethrow().wrap(contentOutput -> {
            contentOutput.writeHeartbeat();
            logger.trace("sent heartbeat to {}", id);
        }));
    }

    private void sendData(String id, Consumer<ContentOutput> contentConsumer) {
        try {
            EventWebhook eventWebhook = outputStreamMap.get(id);
            if (eventWebhook == null) {
                logger.info("unable to find id {}", id);
                unregister(id);
            } else {
                contentConsumer.accept(eventWebhook.getContentOutput());
            }
        } catch (Errors.WrappedAsRuntimeException e) {
            if (e.getCause() instanceof EofException) {
                logger.info("unable to write, closing " + id);
            } else {
                logger.warn("unable to send to " + id, e);
            }
            unregister(id);
        } catch (Exception e) {
            logger.warn("unable to send to " + id, e);
            unregister(id);
        }
    }

    public void register(ContentOutput contentOutput) {
        EventWebhook eventWebhook = new EventWebhook(contentOutput);
        logger.info("registering events {}", eventWebhook.getGroupName());
        outputStreamMap.put(eventWebhook.getGroupName(), eventWebhook);
        eventWebhook.start();
    }

    private void unregister(String id) {
        logger.info("unregistering events {}", id);
        EventWebhook remove = outputStreamMap.remove(id);
        if (null != remove) {
            remove.stop();
        } else {
            webhookService.delete(id);
        }
    }

}
