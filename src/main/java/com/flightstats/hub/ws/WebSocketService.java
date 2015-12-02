package com.flightstats.hub.ws;

import com.flightstats.hub.app.HubHost;
import com.flightstats.hub.app.HubMain;
import com.flightstats.hub.group.Group;
import com.flightstats.hub.group.GroupService;
import com.flightstats.hub.model.ContentKey;
import com.google.inject.Injector;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.websocket.Session;
import java.io.IOException;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

public class WebSocketService {

    private final static Logger logger = LoggerFactory.getLogger(WebSocketService.class);
    private static WebSocketService instance;

    public static synchronized WebSocketService getInstance() {
        if (null == instance) {
            instance = new WebSocketService();
        }
        return instance;
    }

    private final GroupService groupService;
    private final Map<String, Session> sessionMap = new HashMap<>();

    public WebSocketService() {
        Injector injector = HubMain.getInjector();
        groupService = injector.getInstance(GroupService.class);
    }

    public void createCallback(Session session, String channel) throws UnknownHostException {
        ContentKey contentKey = new ContentKey();
        String id = setId(session, channel);
        URI uri = session.getRequestURI();
        logger.info("creating callback {} {} {}", channel, id, uri);
        sessionMap.put(id, session);
        Group group = Group.builder()
                .channelUrl(getChannelUrl(uri))
                .callbackUrl(getCallbackUrl(id))
                .parallelCalls(1)
                .name(id)
                .startingKey(contentKey)
                .build();
        groupService.upsertGroup(group);
    }

    private String getChannelUrl(URI uri) {
        String channelUrl = uri.toString().replaceFirst("ws://", HubHost.getScheme());
        return StringUtils.removeEnd(channelUrl, "/ws");
    }

    private String getCallbackUrl(String id) throws UnknownHostException {
        return HubHost.getLocalHttpIpUri() + "/internal/ws/" + id;
    }

    private String setId(Session session, String channel) {
        Map<String, Object> userProperties = session.getUserProperties();
        String id = "WS_" + channel + "_" + System.currentTimeMillis() + "_" + RandomStringUtils.randomAlphanumeric(6);
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
            logger.info("attempting to send to missing session {} {}", id, uri);
            close(id);
            return;
        }
        try {
            session.getBasicRemote().sendText(uri);
        } catch (IOException e) {
            logger.warn("unable to send to session " + id + " uri " + uri + " " + e.getMessage());
            close(id);
        } catch (Exception e) {
            logger.warn("unable to send to session " + id + " uri " + uri, e);
            close(id);
        }
    }

    public void close(Session session) {
        close(getId(session));
    }

    public void close(String id) {
        try {
            logger.info("deleting ws group {}", id);
            groupService.delete(id);
            sessionMap.remove(id);
        } catch (Exception e) {
            logger.info("unable to close ws group " + id, e);
        }
    }
}
