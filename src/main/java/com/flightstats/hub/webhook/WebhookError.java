package com.flightstats.hub.webhook;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flightstats.hub.dao.ChannelService;
import com.flightstats.hub.model.Content;
import com.flightstats.hub.util.RequestUtils;
import com.flightstats.hub.util.StringUtils;
import com.flightstats.hub.util.TimeUtil;
import com.flightstats.hub.util.SafeZooKeeperUtils;
import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static com.flightstats.hub.util.RequestUtils.getChannelName;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

/**
 * This class is responsible for creation and management of webhook error strings.
 *
 * Format: "{timestamp} {contentKey} {message}"
 *
 * Example: "2018-01-02T03:04:05.006Z 2018/01/02/03/04/05/006/abcdef 400 Bad Request"
 *  - timestamp: 2018-01-02T03:04:05.006Z
 *  - contentKey: 2018/01/02/03/04/05/006/abcdef
 *  - message: 400 Bad Request
 */

@Singleton
class WebhookError {
    private final static Logger logger = LoggerFactory.getLogger(WebhookError.class);
    private static final String BASE_PATH = "/GroupError";

    private final SafeZooKeeperUtils zooKeeperUtils;
    private final ChannelService channelService;
    private final ErrorNodeNameGenerator errorNameGenerator;
    private final WebhookErrorReaper webhookErrorReaper;

    @Inject
    public WebhookError(SafeZooKeeperUtils zooKeeperUtils, ChannelService channelService) {
        this(zooKeeperUtils, channelService, new ErrorNodeNameGenerator(), new WebhookErrorReaper(zooKeeperUtils));
    }

    @VisibleForTesting
    public WebhookError(SafeZooKeeperUtils zooKeeperUtils, ChannelService channelService, ErrorNodeNameGenerator errorNameGenerator, WebhookErrorReaper errorReaper) {
        this.zooKeeperUtils = zooKeeperUtils;
        this.channelService = channelService;
        this.errorNameGenerator = errorNameGenerator;
        this.webhookErrorReaper = errorReaper;
    }

    public void add(String webhook, String error) {
        zooKeeperUtils.createData(error.getBytes(), BASE_PATH, webhook, errorNameGenerator.generateName());
        webhookErrorReaper.limitChildren(webhook);
    }

    public void delete(String webhook) {
        logger.info("deleting webhook errors for " + webhook);
        zooKeeperUtils.deletePathAndChildren(BASE_PATH, webhook);
    }

    public List<String> get(String webhook) {
        return webhookErrorReaper.limitChildren(webhook).stream()
                .map(Error::getData)
                .collect(toList());
    }

    void publishToErrorChannel(DeliveryAttempt attempt) {
        if (attempt.getWebhook().getErrorChannelUrl() == null) return;

        List<String> errors = get(attempt.getWebhook().getName());
        if (errors.size() < 1) {
            logger.debug("no errors found for", attempt.getWebhook().getName());
            return;
        }

        String error = errors.get(errors.size() - 1);
        byte[] bytes = buildPayload(attempt, error);
        Content content = Content.builder()
                .withContentType("application/json")
                .withContentLength((long) bytes.length)
                .withData(bytes)
                .build();

        try {
            channelService.insert(getChannelName(attempt.getWebhook().getErrorChannelUrl()), content);
        } catch (Exception e) {
            logger.warn("unable to publish errors for " + attempt.getWebhook().getName(), e);
        }
    }

    private byte[] buildPayload(DeliveryAttempt attempt, String error) {
        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode root = objectMapper.createObjectNode();
        root.put("webhookUrl", buildWebhookUrl(attempt));
        root.put("failedItemUrl", attempt.getWebhook().getChannelUrl() + "/" + attempt.getContentPath().toUrl());
        root.put("callbackUrl", attempt.getWebhook().getCallbackUrl());
        root.put("numberOfAttempts", attempt.getNumber() - 1);
        root.put("lastAttemptTime", extractTimestamp(error));
        root.put("lastAttemptError", extractMessage(error));
        return root.toString().getBytes();
    }

    private String buildWebhookUrl(DeliveryAttempt attempt) {
        // todo - workaround for HubHost.getLocalNamePort and HubProperties.getAppUrl not returning usable URLs for dockerized single hub
        String host = RequestUtils.getHost(attempt.getWebhook().getChannelUrl());
        return host + "/webhook/" + attempt.getWebhook().getName();
    }

    private String extractTimestamp(String error) {
        return error.substring(0, error.indexOf(" "));
    }

    private String extractMessage(String error) {
        int firstSpace = error.indexOf(" ");
        int secondSpace = error.indexOf(" ", firstSpace + 1);
        return error.substring(secondSpace + 1);
    }

    @VisibleForTesting
    static class ErrorNodeNameGenerator {
        String generateName() {
            return TimeUtil.now().getMillis() + StringUtils.randomAlphaNumeric(6);
        }
    }

    private static class Error {
        String name;
        DateTime creationTime;
        String data;

        @java.beans.ConstructorProperties({"name", "creationTime", "data"})
        Error(String name, DateTime creationTime, String data) {
            this.name = name;
            this.creationTime = creationTime;
            this.data = data;
        }

        public static ErrorBuilder builder() {
            return new ErrorBuilder();
        }

        public String getName() {
            return this.name;
        }

        public DateTime getCreationTime() {
            return this.creationTime;
        }

        public String getData() {
            return this.data;
        }

        public static class ErrorBuilder {
            private String name;
            private DateTime creationTime;
            private String data;

            ErrorBuilder() {
            }

            public Error.ErrorBuilder name(String name) {
                this.name = name;
                return this;
            }

            public Error.ErrorBuilder creationTime(DateTime creationTime) {
                this.creationTime = creationTime;
                return this;
            }

            public Error.ErrorBuilder data(String data) {
                this.data = data;
                return this;
            }


            public Error build() {
                return new Error(name, creationTime, data);
            }

            public String toString() {
                return "com.flightstats.hub.webhook.WebhookError.Error.ErrorBuilder(name=" + this.name + ", creationTime=" + this.creationTime + ", data=" + this.data + ")";
            }
        }
    }

    static class WebhookErrorReaper {
        private static final int MAX_SIZE = 10;
        private static final Duration MAX_AGE = Duration.ofDays(1);

        private final SafeZooKeeperUtils zooKeeperUtils;

        @Inject
        WebhookErrorReaper(SafeZooKeeperUtils zooKeeperUtils) {
            this.zooKeeperUtils = zooKeeperUtils;
        }

        List<Error> limitChildren(String webhook) {
            try {
                List<Error> errors = getChildren(webhook);
                List<Error> errorsToDelete = getErrorsToRemove(errors);

                errorsToDelete.forEach(error -> zooKeeperUtils.deletePathInBackground(BASE_PATH, webhook, error.getName()));

                return errors.stream()
                        .filter(error -> !errorsToDelete.contains(error))
                        .collect(toList());
            } catch (Exception e) {
                logger.warn("unable to limit webhook error children " + webhook, e);
            }
            return emptyList();
        }

        private List<Error> getChildren(String webhook) {
             return zooKeeperUtils.getChildren(BASE_PATH, webhook).stream()
                    .map(child -> zooKeeperUtils.getDataWithStat(BASE_PATH, webhook, child)
                            .map(dataWithStat -> new Error(child, new DateTime(dataWithStat.getStat().getCtime()), dataWithStat.getData())))
                    .flatMap(maybeData -> maybeData.map(Stream::of).orElse(Stream.empty()))
                    .collect(toList());

        }

        private List<Error> getErrorsToRemove(List<Error> errors) {
            return IntStream.range(0, errors.size())
                    .filter(errorIndex -> shouldRemoveError(errors, errorIndex))
                    .mapToObj(errors::get)
                    .collect(toList());
        }

        private boolean shouldRemoveError(List<Error> errors, int errorIndex) {
            DateTime cutoffTime = TimeUtil.now().minus(MAX_AGE.toMillis());
            int maxErrorIndexToDelete = errors.size() - MAX_SIZE;
            return errorIndex < maxErrorIndexToDelete || errors.get(errorIndex).getCreationTime().isBefore(cutoffTime);
        }
    }
}
