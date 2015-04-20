package com.flightstats.hub.alert;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flightstats.hub.app.HubProperties;
import com.flightstats.hub.rest.RestClient;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.util.LinkedList;

/**
 * Keep the state for an alert, send alert if it meets the requirements.
 */
@Getter
public class AlertChecker {

    private final static Logger logger = LoggerFactory.getLogger(AlertChecker.class);

    public static final ScriptEngine jsEngine = createJsEngine();
    private static final Client client = RestClient.createClient(15, 60);
    private static final ObjectMapper mapper = new ObjectMapper();

    private static String alertChannelEscalate = HubProperties.getProperty("alert.channel.escalate", "escalationAlerts");

    private final AlertConfig alertConfig;
    private final LinkedList<JsonNode> history = new LinkedList<>();
    private boolean isTriggered = false;

    public AlertChecker(AlertConfig alertConfig) {
        this.alertConfig = alertConfig;
    }

    public boolean start() {
        return start(false);
    }

    public boolean start(boolean status) {
        isTriggered = status;
        try {
            logger.debug("start alertConfig {}", alertConfig);
            JsonNode json = getJson(alertConfig.getHubDomain() + "channel/" + alertConfig.getChannel() + "/time/minute");
            while (history.size() < alertConfig.getTimeWindowMinutes()) {
                json = getJson(json.get("_links").get("previous").get("href").asText());
                history.addFirst(json);
            }
            logger.trace("start history {}", history);
            checkForAlert();
            return true;
        } catch (Exception e) {
            logger.warn("unable to start " + alertConfig, e);
            return false;
        }
    }

    public void update() {
        try {
            boolean updateNext = true;
            logger.trace("update history {}", history);
            while (updateNext) {
                JsonNode last = history.getLast();
                logger.debug("last {}", last);
                JsonNode nextJson = getJson(last.get("_links").get("next").get("href").asText());
                if (nextJson.get("_links").has("next")) {
                    history.removeFirst();
                    history.add(nextJson);
                    checkForAlert();
                } else {
                    updateNext = false;
                }
            }
        } catch (Exception e) {
            logger.warn("unable to update " + alertConfig + " " + history, e);
        }
    }

    boolean checkForAlert() throws ScriptException {
        int count = history.stream()
                .mapToInt(node -> node.get("_links").get("uris").size())
                .sum();
        String script = count + " " + alertConfig.getOperator() + " " + alertConfig.getThreshold();
        Boolean evaluate = (Boolean) jsEngine.eval(script);
        logger.debug("check for alert {} {} {}", alertConfig.getName(), script, evaluate);
        if (!isTriggered && evaluate) {
            sendAlert(count);
        }
        isTriggered = evaluate;
        return evaluate;
    }

    private void sendAlert(int count) {
        String url = alertConfig.getHubDomain() + "channel/" + alertChannelEscalate;
        ObjectNode alert = mapper.createObjectNode();
        alert.put("serviceName", alertConfig.getServiceName());
        alert.put("description", alertConfig.getName() + ": " +
                alertConfig.getHubDomain() + "/channel/" + alertConfig.getChannel() + " volume " +
                count + " " + alertConfig.getOperator() + " " + alertConfig.getThreshold());
        alert.put("details", "TBD");
        String entity = alert.toString();
        logger.info("sending alert {}", entity);
        client.resource(url)
                .type(MediaType.APPLICATION_JSON_TYPE)
                .post(entity);
    }

    private JsonNode getJson(String url) throws IOException {
        ClientResponse response = client.resource(url).get(ClientResponse.class);
        JsonNode jsonNode = mapper.readTree(response.getEntity(String.class));
        logger.debug("called {} response {} {}", url, response.getStatus(), jsonNode);
        return jsonNode;
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
        for (JsonNode node : history) {
            ObjectNode objectNode = historyNode.addObject();
            objectNode.put("href", node.get("_links").get("self").get("href").asText());
            objectNode.put("items", node.get("_links").get("uris").size());
        }

        alertNode.put("alert", isTriggered);
    }

}
