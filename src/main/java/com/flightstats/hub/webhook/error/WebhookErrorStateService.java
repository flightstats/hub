package com.flightstats.hub.webhook.error;

import com.flightstats.hub.util.SafeZooKeeperUtils;
import com.flightstats.hub.util.StringUtils;
import com.flightstats.hub.util.TimeUtil;
import com.google.common.annotations.VisibleForTesting;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

public class WebhookErrorStateService {
    private static final Logger logger = LoggerFactory.getLogger(WebhookErrorStateService.class);
    private static final String BASE_PATH = "/GroupError";

    private final SafeZooKeeperUtils zooKeeperUtils;
    private final ErrorNodeNameGenerator errorNameGenerator;

    @Inject
    public WebhookErrorStateService(SafeZooKeeperUtils zooKeeperUtils, ErrorNodeNameGenerator errorNameGenerator) {
        this.zooKeeperUtils = zooKeeperUtils;
        this.errorNameGenerator = errorNameGenerator;
    }

    public void add(String webhook, String error) {
        zooKeeperUtils.createData(error.getBytes(), BASE_PATH, webhook, errorNameGenerator.generateName());
    }

    public void deleteWebhook(String webhook) {
        logger.info("deleting webhook errors for " + webhook);
        zooKeeperUtils.deletePathAndChildren(BASE_PATH, webhook);
    }

    public void delete(String webhook, String errorId) {
        zooKeeperUtils.deletePathInBackground(BASE_PATH, webhook, errorId);
    }

    public Set<String> getWebhooks() {
        return new HashSet<>(zooKeeperUtils.getChildren(BASE_PATH));
    }

    public List<WebhookError> getErrors(String webhook) {
        return zooKeeperUtils.getChildren(BASE_PATH, webhook).stream()
                .map(child -> zooKeeperUtils.getDataWithStat(BASE_PATH, webhook, child)
                        .map(dataWithStat -> WebhookError.builder()
                                .name(child)
                                .creationTime(new DateTime(dataWithStat.getStat().getCtime()))
                                .data(dataWithStat.getData())
                                .build()))
                .flatMap(maybeData -> maybeData.map(Stream::of).orElse(Stream.empty()))
                .collect(toList());

    }

    @VisibleForTesting
    public static class ErrorNodeNameGenerator {
        public String generateName() {
            return TimeUtil.now().getMillis() + StringUtils.randomAlphaNumeric(6);
        }
    }
}
