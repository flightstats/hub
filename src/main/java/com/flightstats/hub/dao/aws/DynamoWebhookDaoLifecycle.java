package com.flightstats.hub.dao.aws;

import com.flightstats.hub.config.properties.AppProperties;
import com.flightstats.hub.config.properties.DynamoProperties;
import com.google.common.util.concurrent.AbstractIdleService;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;

@Slf4j
public class DynamoWebhookDaoLifecycle extends AbstractIdleService {
    private final DynamoUtils dynamoUtils;
    private final AppProperties appProperties;
    private final DynamoProperties dynamoProperties;

    @Inject
    public DynamoWebhookDaoLifecycle(DynamoUtils dynamoUtils, AppProperties appProperties, DynamoProperties dynamoProperties) {
        this.dynamoUtils = dynamoUtils;
        this.appProperties = appProperties;
        this.dynamoProperties = dynamoProperties;
    }

    @Override
    protected void startUp() {
        initialize();
    }

    @Override
    protected void shutDown() {
    }

    private void initialize() {
        if (appProperties.isReadOnly()) {
            if (!dynamoUtils.doesTableExist(dynamoProperties.getWebhookConfigTableName())) {
                String msg = String.format(
                        "Probably fatal error. Dynamo webhook config table doesn't exist for r/o node.  %s",
                        dynamoProperties.getWebhookConfigTableName());
                log.error(msg);
                throw new IllegalArgumentException(msg);
            }
        } else {
            dynamoUtils.createAndUpdate(dynamoProperties.getWebhookConfigTableName(), "webhook", "name");
        }
    }

}
