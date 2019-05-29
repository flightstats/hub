package com.flightstats.hub.ws;

import com.flightstats.hub.config.properties.LocalHostProperties;
import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.util.StringUtils;
import com.flightstats.hub.webhook.Webhook;
import com.flightstats.hub.webhook.WebhookService;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.websocket.Session;
import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class WebSocketService {

    private final Map<String, Session> sessionMap = new HashMap<>();

    private final WebhookService webhookService;
    private LocalHostProperties localHostProperties;

    @Inject
    public WebSocketService(WebhookService webhookService, LocalHostProperties localHostProperties) {
        this.webhookService = webhookService;
        this.localHostProperties = localHostProperties;
    }

    void createCallback(Session session, String channel) {
        createCallback(session, channel, new ContentKey());
    }

    void createCallback(Session session, String channel, ContentKey startingKey) {
        String id = setId(session, channel);
        URI uri = session.getRequestURI();
        log.info("creating callback {} {} {}", channel, id, uri);
        sessionMap.put(id, session);
        Webhook webhook = Webhook.builder()
                .channelUrl(getChannelUrl(uri))
                .callbackUrl(getCallbackUrl(id))
                .parallelCalls(1)
                .name(id)
                .startingKey(startingKey)
                .build();
        webhookService.upsert(webhook);
    }

    private String getChannelUrl(URI uri) {
        int indexBefore = "/channel/".length();
        int indexAfter = uri.getPath().indexOf("/", indexBefore);
        String channelPath = uri.getPath().substring(0, indexAfter);
        String serverURL = localHostProperties.getUriScheme() + uri.getAuthority();
        URI channelUrl = UriBuilder.fromUri(serverURL).path(channelPath).build();
        return channelUrl.toString();
    }

    private String getCallbackUrl(String id) {
        return localHostProperties.getUriWithHostIp() + "/internal/ws/" + id;
    }

    private String setId(Session session, String channel) {
        Map<String, Object> userProperties = session.getUserProperties();
        String id = "WS_" + channel + "_" + System.currentTimeMillis() + "_" + StringUtils.randomAlphaNumeric(6);
        userProperties.put("id", id);
        return id;
    }

    private String getId(Session session) {
        Map<String, Object> userProperties = session.getUserProperties();
        return (String) userProperties.get("id");
    }

    public void call(String id, String uri) {
        Session session = sessionMap.get(id);
        if (session == null) {
            log.info("attempting to send to missing session {} {}", id, uri);
            close(id);
            return;
        }
        try {
            session.getBasicRemote().sendText(uri);
        } catch (IOException e) {
            log.warn("unable to send to session " + id + " uri " + uri + " " + e.getMessage());
            close(id);
        } catch (Exception e) {
            log.warn("unable to send to session " + id + " uri " + uri, e);
            close(id);
        }
    }

    public void close(Session session) {
        close(getId(session));
    }

    private void close(String id) {
        try {
            log.info("deleting ws group {}", id);
            webhookService.delete(id);
            sessionMap.remove(id);
        } catch (Exception e) {
            log.info("unable to close ws group " + id, e);
        }
    }
}
