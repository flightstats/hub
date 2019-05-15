package com.flightstats.hub.dao.aws;

import com.flightstats.hub.config.DynamoProperties;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DynamoChannelConfigDaoLifecycle extends AbstractIdleService {
    private final DynamoUtils dynamoUtils;
    private final DynamoProperties dynamoProperties;

    @Inject
    public DynamoChannelConfigDaoLifecycle(DynamoUtils dynamoUtils, DynamoProperties dynamoProperties) {
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

    void initialize() {
        String tableName = dynamoProperties.getChannelConfigTableName();

        if (!dynamoUtils.doesTableExist(tableName)) {
            String msg = String.format("Dynamo channel config table doesn't exist.  %s", tableName);
            log.error(msg);
            throw new IllegalArgumentException(msg);
        }
    }

}