package com.flightstats.hub.dao.aws;

import com.flightstats.hub.config.DynamoProperties;
import com.google.common.util.concurrent.AbstractIdleService;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;

@Slf4j
public class DynamoWebhookExistenceCheck extends AbstractIdleService {
    private final DynamoUtils dynamoUtils;
    private final DynamoProperties dynamoProperties;

    @Inject
    public DynamoWebhookExistenceCheck(DynamoUtils dynamoUtils, DynamoProperties dynamoProperties) {
        this.dynamoUtils = dynamoUtils;
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
        if (!dynamoUtils.doesTableExist(dynamoProperties.getWebhookConfigTableName())) {
            String msg = String.format(
                    "Dynamo webhook config table doesn't exist.  %s",
                    dynamoProperties.getWebhookConfigTableName());
            log.error(msg);
            throw new IllegalArgumentException(msg);
        }
    }

}
