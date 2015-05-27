package com.flightstats.hub.alert;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flightstats.hub.app.HubProperties;
import com.flightstats.hub.cluster.BooleanValue;
import com.flightstats.hub.rest.RestClient;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import lombok.Getter;
import org.apache.curator.framework.CuratorFramework;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Keep the state for an alert, send alert if it meets the requirements.
 */
@Getter
//todo - gfm - 5/27/15 - delete
public class AlertChecker {

    private final static Logger logger = LoggerFactory.getLogger(AlertChecker.class);

    public static final ScriptEngine jsEngine = createJsEngine();
    private static final Client client = RestClient.createClient(15, 60);
    private static final ObjectMapper mapper = new ObjectMapper();

    private static String alertChannelEscalate = HubProperties.getProperty("alert.channel.escalate", "escalationAlerts");

    private final AlertConfig alertConfig;
    private CuratorFramework curator;
    private final LinkedList<AlertHistory> history = new LinkedList<>();
    private final BooleanValue isTriggered;
    private AtomicBoolean inProcess = new AtomicBoolean(false);

    public AlertChecker(AlertConfig alertConfig, CuratorFramework curator) {
        this.alertConfig = alertConfig;
        this.curator = curator;
        isTriggered = new BooleanValue(curator);
    }

    public synchronized void start() {
        try {
            logger.info("starting {}", alertConfig.getName());
            inProcess.set(true);
            logger.debug("start alertConfig {}", alertConfig);
            AlertHistory alertHistory = getAlertHistory(alertConfig.getHubDomain() + "channel/" + alertConfig.getChannel() + "/time/minute");
            while (history.size() < alertConfig.getTimeWindowMinutes()) {
                alertHistory = getAlertHistory(alertHistory.getPrevious());
                history.addFirst(alertHistory);
            }
            logger.trace("start history {}", history);
            checkForAlert();
        } catch (Exception e) {
            logger.warn("unable to start " + alertConfig, e);
        } finally {
            inProcess.set(false);
            logger.info("started {}", alertConfig.getName());
        }
    }

    public synchronized void update() {
        try {
            logger.info("updating {}", alertConfig.getName());
            inProcess.set(true);
            boolean updateNext = true;
            logger.trace("update history {}", history);
            while (updateNext) {
                AlertHistory lastHistory = history.getLast();
                logger.debug("last {}", lastHistory);
                if (lastHistory.hasNext()) {
                    AlertHistory nextHistory = getAlertHistory(lastHistory.getNext());
                    if (nextHistory.hasNext()) {
                        history.removeFirst();
                        history.add(nextHistory);
                        checkForAlert();
                    } else {
                        updateNext = false;
                    }
                } else {
                    updateNext = false;
                }
            }
        } catch (Exception e) {
            logger.warn("unable to update " + alertConfig + " " + history, e);
        } finally {
            logger.info("updated {}", alertConfig.getName());
            inProcess.set(false);
        }
    }

    public boolean inProcess() {
        return inProcess.get();
    }

    boolean checkForAlert() throws ScriptException {
        int count = history.stream()
                .mapToInt(AlertHistory::getCount)
                .sum();
        String script = count + " " + alertConfig.getOperator() + " " + alertConfig.getThreshold();
        Boolean evaluate = (Boolean) jsEngine.eval(script);
        logger.debug("check for alert {} {} {}", alertConfig.getName(), script, evaluate);
        if (evaluate) {
            if (isTriggered.setIfNotValue(getPath(), true)) {
                sendAlert(count);
            }
        } else {
            isTriggered.setIfNotValue(getPath(), false);
        }

        return evaluate;
    }

    @NotNull
    private String getPath() {
        return "/AlertChecker/" + alertConfig.getName();
    }

    private void sendAlert(int count) {
        String url = alertConfig.getHubDomain() + "channel/" + alertChannelEscalate;
        ObjectNode alert = mapper.createObjectNode();
        alert.put("serviceName", alertConfig.getServiceName());
        alert.put("description", alertConfig.getName() + ": " +
                alertConfig.getHubDomain() + "channel/" + alertConfig.getChannel() + " volume " +
                count + " " + alertConfig.getOperator() + " " + alertConfig.getThreshold());
        toJson(alert.putObject("details"));
        String entity = alert.toString();
        logger.info("sending alert {}", entity);
        client.resource(url)
                .type(MediaType.APPLICATION_JSON_TYPE)
                .post(entity);
    }

    private AlertHistory getAlertHistory(String url) throws IOException {
        ClientResponse response = client.resource(url).get(ClientResponse.class);
        JsonNode jsonNode = mapper.readTree(response.getEntity(String.class));
        logger.debug("called {} response {} {}", url, response.getStatus(), jsonNode);
        JsonNode links = jsonNode.get("_links");
        AlertHistory.AlertHistoryBuilder builder = AlertHistory.builder()
                .count(links.get("uris").size())
                .previous(links.get("previous").get("href").asText())
                .self(links.get("self").get("href").asText());
        if (links.has("next")) {
            builder.next(links.get("next").get("href").asText());
        }

        return builder.build();
    }

    private static ScriptEngine createJsEngine() {
        ScriptEngineManager engineManager = new ScriptEngineManager();
        return engineManager.getEngineByName("nashorn");
    }

    public void toJson(ObjectNode root) {
        String name = getAlertConfig().getName();
        ObjectNode alertNode = root.putObject(name);
        alertNode.put("name", name);
        ArrayNode historyNode = alertNode.putArray("history");
        for (AlertHistory node : history) {
            ObjectNode objectNode = historyNode.addObject();
            objectNode.put("href", node.getSelf());
            objectNode.put("items", node.getCount());
        }

        alertNode.put("alert", isTriggered.get(getPath(), false));
    }

}
