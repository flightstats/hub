package com.flightstats.hub.alert;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flightstats.hub.group.Group;
import com.flightstats.hub.group.GroupStatus;
import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.rest.RestClient;
import com.sun.jersey.api.client.Client;
import org.joda.time.Minutes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.concurrent.Callable;

public class GroupAlertUpdater implements Callable<AlertStatus> {

    private final static Logger logger = LoggerFactory.getLogger(GroupAlertUpdater.class);

    private static final Client client = RestClient.defaultClient();
    private static final ObjectMapper mapper = new ObjectMapper();

    private final AlertConfig alertConfig;
    private final AlertStatus alertStatus;

    public GroupAlertUpdater(AlertConfig alertConfig, AlertStatus alertStatus) {
        this.alertConfig = alertConfig;
        if (alertStatus == null) {
            alertStatus = AlertStatus.builder()
                    .name(alertConfig.getName())
                    .alert(false)
                    .history(new LinkedList<>())
                    .build();
        }
        this.alertStatus = alertStatus;
        alertStatus.setType(alertConfig.getAlertType().name());
    }

    @Override
    public AlertStatus call() throws Exception {
        alertStatus.getHistory().clear();
        GroupStatus groupStatus = GroupState.getGroupStatus(alertConfig);

        if (groupStatus == null) {
            return alertStatus;
        }
        ContentKey channelLatest = groupStatus.getChannelLatest();
        if (channelLatest == null) {
            return alertStatus;
        }
        addHistory(channelLatest, groupStatus.getGroup(), "channelLatest");
        ContentKey lastCompleted = groupStatus.getLastCompleted();
        addHistory(lastCompleted, groupStatus.getGroup(), "lastCompletedCallback");
        Minutes minutes = Minutes.minutesBetween(lastCompleted.getTime(), channelLatest.getTime());
        logger.trace("alert {} latest {} completed {} minutes {}", alertConfig.getName(), channelLatest, lastCompleted, minutes);
        if (minutes.getMinutes() >= alertConfig.getTimeWindowMinutes()) {
            if (!alertStatus.isAlert()) {
                alertStatus.setAlert(true);
                AlertSender.sendAlert(alertConfig, alertStatus, minutes.getMinutes());
            }
        } else {
            alertStatus.setAlert(false);
        }
        return alertStatus;
    }

    private void addHistory(ContentKey contentKey, Group group, String name) {
        AlertStatusHistory history = AlertStatusHistory.builder()
                .href(group.getChannelUrl() + "/" + contentKey.toUrl())
                .name(name)
                .items(0)
                .build();
        alertStatus.getHistory().add(history);
    }

}
