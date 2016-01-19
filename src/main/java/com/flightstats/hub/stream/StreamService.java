package com.flightstats.hub.stream;

import com.diffplug.common.base.Errors;
import com.flightstats.hub.dao.ContentService;
import com.flightstats.hub.model.ChannelContentKey;
import com.flightstats.hub.model.Content;
import com.flightstats.hub.util.HubUtils;
import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.eclipse.jetty.io.EofException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.MediaType;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

@Singleton
public class StreamService {

    private final static Logger logger = LoggerFactory.getLogger(StreamService.class);
    public static final MediaType MULTIPART_STREAM = new MediaType("multipart", "stream");

    @Inject
    private ContentService contentService;
    @Inject
    private HubUtils hubUtils;
    private Map<String, CallbackStream> outputStreamMap = new ConcurrentHashMap<>();

    public void getAndSendData(String uri, String id) {
        logger.trace("got uri {} {}", uri, id);
        ChannelContentKey key = ChannelContentKey.fromUrl(uri);
        if (key != null) {
            Optional<Content> optional = contentService.getValue(key.getChannel(), key.getContentKey());
            if (optional.isPresent()) {
                Content content = optional.get();
                sendData(id, Errors.rethrow().wrap(contentOutput -> {
                    contentOutput.write(content);
                    logger.trace("sent content {} to {}", id, content.getContentKey());
                }));
            }
        }
    }

    public void checkHealth(String id) {
        logger.trace("check health {}", id);
        sendData(id, Errors.rethrow().wrap(contentOutput -> {
            contentOutput.writeHeartbeat();
            logger.trace("sent heartbeat to {}", id);
        }));
    }

    private void sendData(String id, Consumer<ContentOutput> contentConsumer) {
        try {
            CallbackStream callbackStream = outputStreamMap.get(id);
            if (callbackStream == null) {
                logger.info("unable to find id {}", id);
                unregister(id);
            } else {
                contentConsumer.accept(callbackStream.getContentOutput());
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
        CallbackStream callbackStream = new CallbackStream(contentOutput, hubUtils);
        logger.info("registering stream {}", callbackStream.getGroupName());
        outputStreamMap.put(callbackStream.getGroupName(), callbackStream);
        callbackStream.start();
    }

    public void unregister(String id) {
        logger.info("unregistering stream {}", id);
        CallbackStream remove = outputStreamMap.remove(id);
        if (null != remove) {
            remove.stop();
        }
    }

}
