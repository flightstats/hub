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
import java.util.concurrent.Callable;

public class ChannelAlertUpdater implements Callable<AlertStatus> {

    private final static Logger logger = LoggerFactory.getLogger(ChannelAlertUpdater.class);
    private static final ScriptEngine jsEngine = createJsEngine();
    private static final Client client = RestClient.defaultClient();
    private static final ObjectMapper mapper = new ObjectMapper();

    private final AlertConfig alertConfig;
    private final AlertStatus alertStatus;

    public ChannelAlertUpdater(AlertConfig alertConfig, AlertStatus alertStatus) {
        this.alertConfig = alertConfig;
        if (alertStatus == null) {
            alertStatus = AlertStatus.builder()
                    .name(alertConfig.getName())
                    .alert(false)
                    .period(AlertStatus.MINUTE)
                    .history(new LinkedList<>())
                    .build();
        }
        this.alertStatus = alertStatus;
    }

    @Override
    public AlertStatus call() throws Exception {
        int historyCount = alertConfig.getTimeWindowMinutes();
        if (alertConfig.getTimeWindowMinutes() < 120) {
            checkPeriod(AlertStatus.MINUTE);
        } else {
            checkPeriod(AlertStatus.HOUR);
            historyCount = (int) Math.ceil(alertConfig.getTimeWindowMinutes() / 60.0);
        }
        while (alertStatus.getHistory().size() > historyCount) {
            alertStatus.getHistory().removeFirst();
        }

        LinkedList<AlertStatusHistory> history = alertStatus.getHistory();
        if (history.isEmpty()) {
            AlertStatusHistory alertStatusHistory = getAlertHistory(alertConfig.getHubDomain() +
                    "channel/" + alertConfig.getChannel() + "/time/" + alertStatus.getPeriod());
            while (history.size() < historyCount) {
                alertStatusHistory = getAlertHistory(alertStatusHistory.getPrevious());
                history.addFirst(alertStatusHistory);
            }
            checkForAlert();
        } else {
            AlertStatusHistory alertHistory = getAlertHistory(history.getLast().getHref());
            while (alertHistory.getNext() != null) {
                alertHistory = getAlertHistory(alertHistory.getNext());
                if (alertHistory.getNext() != null) {
                    history.addLast(alertHistory);
                    if (history.size() > historyCount) {
                        history.removeFirst();
                    }
                }
            }
            checkForAlert();
        }

        return alertStatus;
    }

    boolean checkForAlert() throws ScriptException {
        int count = alertStatus.getHistory().stream()
                .mapToInt(AlertStatusHistory::getItems)
                .sum();
        String script = count + " " + alertConfig.getOperator() + " " + alertConfig.getThreshold();
        Boolean evaluate = (Boolean) jsEngine.eval(script);
        logger.debug("check for alert {} {} {}", alertConfig.getName(), script, evaluate);
        if (evaluate && !alertStatus.isAlert()) {
            alertStatus.setAlert(true);
            AlertSender.sendAlert(alertConfig, alertStatus, count);
        }
        alertStatus.setAlert(evaluate);
        return evaluate;
    }

    private void checkPeriod(String period) {
        if (!period.equals(alertStatus.getPeriod())) {
            logger.info("clearing history {}", alertConfig);
            alertStatus.getHistory().clear();
        }
        alertStatus.setPeriod(period);
    }

    private AlertStatusHistory getAlertHistory(String url) throws IOException {
        logger.debug("calling {}", url);
        ClientResponse response = client.resource(url).get(ClientResponse.class);
        JsonNode jsonNode = mapper.readTree(response.getEntity(String.class));
        logger.trace("called {} response {} {}", url, response.getStatus(), jsonNode);
        JsonNode links = jsonNode.get("_links");
        AlertStatusHistory.AlertStatusHistoryBuilder builder = AlertStatusHistory.builder()
                .items(links.get("uris").size())
                .previous(links.get("previous").get("href").asText())
                .href(links.get("self").get("href").asText());
        if (links.has("next")) {
            builder.next(links.get("next").get("href").asText());
        }

        return builder.build();
    }

    private static ScriptEngine createJsEngine() {
        ScriptEngineManager engineManager = new ScriptEngineManager();
        return engineManager.getEngineByName("nashorn");
    }
}
