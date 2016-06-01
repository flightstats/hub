package com.flightstats.hub.alert;

import com.flightstats.hub.group.Group;
import com.flightstats.hub.group.GroupStatus;
import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.model.ContentPath;
import org.joda.time.Minutes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.concurrent.Callable;

class GroupAlertUpdater implements Callable<AlertStatus> {

    private final static Logger logger = LoggerFactory.getLogger(GroupAlertUpdater.class);

    private final AlertConfig alertConfig;
    private final AlertStatus alertStatus;

    GroupAlertUpdater(AlertConfig alertConfig, AlertStatus alertStatus) {
        this.alertConfig = alertConfig;
        if (alertStatus == null) {
            alertStatus = AlertStatus.builder()
                    .name(alertConfig.getName())
                    .alert(false)
                    .history(new LinkedList<>())
                    .build();
        }
        this.alertStatus = alertStatus;
        alertStatus.setType(AlertConfig.AlertType.group.name());
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
        ContentPath lastCompleted = groupStatus.getLastCompleted();
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

    private void addHistory(ContentPath contentPath, Group group, String name) {
        AlertStatusHistory history = AlertStatusHistory.builder()
                .href(group.getChannelUrl() + "/" + contentPath.toUrl())
                .name(name)
                .build();
        alertStatus.getHistory().add(history);
    }

}
