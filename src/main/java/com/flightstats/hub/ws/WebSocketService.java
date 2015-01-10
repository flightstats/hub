package com.flightstats.hub.ws;

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
import java.net.InetAddress;
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
        String id = session.getId();
        URI uri = session.getRequestURI();
        logger.info("creating callback {} {} {}", channel, id, uri);
        sessionMap.put(id, session);
        String groupName = setGroupName(session, channel);
        Group group = Group.builder()
                .channelUrl(getChannelUrl(uri))
                .callbackUrl(getCallbackUrl(id, uri))
                .parallelCalls(1)
                .name(groupName)
                .startingKey(contentKey)
                .build();
        groupService.upsertGroup(group);
    }

    private String getChannelUrl(URI uri) {
        String channelUrl = uri.toString().replaceFirst("ws://", "http://");
        channelUrl = StringUtils.removeEnd(channelUrl, "/ws");
        return channelUrl;
    }

    private String getCallbackUrl(String id, URI uri) throws UnknownHostException {
        return "http://" + InetAddress.getLocalHost().getHostAddress() + ":" + uri.getPort() + "/ws/" + id;
    }

    private String setGroupName(Session session, String channel) {
        Map<String, Object> userProperties = session.getUserProperties();
        String groupName = "WS_" + channel + "_" + RandomStringUtils.randomAlphanumeric(20);
        userProperties.put("groupName", groupName);
        return groupName;
    }

    private String getGroupName(Session session) {
        Map<String, Object> userProperties = session.getUserProperties();
        return (String) userProperties.get("groupName");
    }

    public void call(String id, String uri) {
        Session session = sessionMap.get(id);
        if (session == null) {
            logger.info("attempting to send to missing session {} {}", id, uri);
            return;
        }
        try {
            session.getBasicRemote().sendText(uri);
        } catch (IOException e) {
            logger.warn("unable to send to session " + id + " uri " + uri + " " + e.getMessage());
            close(session);
        } catch (Exception e) {
            logger.warn("unable to send to session " + id + " uri " + uri, e);
            close(session);
        }
    }

    public void close(Session session) {
        String groupName = getGroupName(session);
        logger.info("deleting group {}", groupName);
        groupService.delete(groupName);
        sessionMap.remove(session.getId());
    }
}
