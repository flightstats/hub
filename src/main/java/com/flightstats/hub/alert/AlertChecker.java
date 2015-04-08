package com.flightstats.hub.alert;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flightstats.hub.rest.RestClient;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.IOException;
import java.util.LinkedList;

/**
 * Keep the state for an alert, send alert if it meets the requirements.
 */
public class AlertChecker {

    private final static Logger logger = LoggerFactory.getLogger(AlertChecker.class);

    public static final ScriptEngine jsEngine = createJsEngine();
    private final static Client client = RestClient.createClient(15, 60);

    private final static ObjectMapper mapper = new ObjectMapper();

    private final AlertConfig alertConfig;
    private final LinkedList<JsonNode> history = new LinkedList<>();

    public AlertChecker(AlertConfig alertConfig) {
        this.alertConfig = alertConfig;
    }

    public void start() throws Exception {
        JsonNode json = getJson(alertConfig.getHubDomain() + "/channel/" + alertConfig.getChannel() + "/time/minute");
        while (history.size() < alertConfig.getMinutes()) {
            json = getJson(json.get("_links").get("previous").get("href").asText());
            history.addFirst(json);
        }
        checkForAlert();
    }

    public void update() throws IOException, ScriptException {
        boolean updateNext = true;
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
    }

    boolean checkForAlert() throws ScriptException {
        int count = history.stream()
                .mapToInt(node -> node.get("_links").get("uris").size())
                .sum();
        String script = count + " " + alertConfig.getOperator() + " " + alertConfig.getThreshold();
        Boolean evaluate = (Boolean) jsEngine.eval(script);
        logger.debug("check for alert {} {} {}", alertConfig.getName(), script, evaluate);
        return evaluate;
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
}
