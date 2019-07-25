package com.flightstats.hub.system.service;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.AttributeAction;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.AttributeValueUpdate;
import com.flightstats.hub.model.ChannelConfigExpirationSettings;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.joda.time.base.BaseDateTime;

import javax.inject.Inject;
import javax.inject.Named;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import static com.flightstats.hub.system.config.PropertiesName.DYNAMODB_CHANNEL_CONFIG_TABLE;
import static com.flightstats.hub.system.config.PropertiesName.HELM_RELEASE_NAME;

@Slf4j
public class DynamoDbService {
    private final AmazonDynamoDB dynamoClient;
    private final String releaseName;
    private final String channelConfigTableTemplate;

    @Inject
    public DynamoDbService(AmazonDynamoDB dynamoClient,
                           @Named(HELM_RELEASE_NAME) String releaseName,
                           @Named(DYNAMODB_CHANNEL_CONFIG_TABLE) String channelConfigTableTemplate) {
        this.dynamoClient = dynamoClient;
        this.releaseName = releaseName;
        this.channelConfigTableTemplate = channelConfigTableTemplate;
    }

    public void updateChannelConfig(ChannelConfigExpirationSettings expirationSettings) {
        Map<String, AttributeValue> query = new HashMap();
        query.put("key", new AttributeValue().withS(expirationSettings.getChannelName().toLowerCase()));
        dynamoClient.updateItem(getChannelConfigTable(), query, getUpdatedFields(expirationSettings));

    }

    private String getChannelConfigTable() {
        return String.format(channelConfigTableTemplate, releaseName);
    }

    private Map<String, AttributeValueUpdate> getUpdatedFields(ChannelConfigExpirationSettings config) {
        Map<String, AttributeValueUpdate> item = new HashMap<>();
        addAttributeUpdate(item, "keepForever", getAttributeValueFromBool(config.isKeepForever()));
        addAttributeUpdate(item, "ttlDays", getAttributeValueFromLong(config.getTtlDays()));
        addAttributeUpdate(item, "maxItems", getAttributeValueFromLong(config.getMaxItems()));
        addAttributeUpdate(item, "mutableTime", getAttributeValueFromDateTime(config.getMutableTime()));

        return item;
    }

    private Optional<AttributeValue> getAttributeValueFromDateTime(Optional<DateTime> dateTime) {
        return dateTime
                .map(BaseDateTime::getMillis)
                .map(this::getAttributeValueFromLong);
    }

    private AttributeValue getAttributeValueFromLong(long number) {
        return new AttributeValue().withN(String.valueOf(number));
    }

    private AttributeValue getAttributeValueFromBool(boolean bool) {
        return new AttributeValue().withBOOL(bool);
    }

    private void addAttributeUpdate(Map<String, AttributeValueUpdate> item, String key, Optional<AttributeValue> attributeValue) {
        if (attributeValue.isPresent()) {
            addAttributeUpdate(item, key, attributeValue.get());
        } else {
            deleteAttribute(item, key);
        }
    }

    private void addAttributeUpdate(Map<String, AttributeValueUpdate> item, String key, AttributeValue attributeValue) {
        AttributeValueUpdate valueUpdate = new AttributeValueUpdate();
        valueUpdate.setValue(attributeValue);
        item.put(key, valueUpdate);
    }

    private void deleteAttribute(Map<String, AttributeValueUpdate> item, String key) {
        AttributeValueUpdate valueUpdate = new AttributeValueUpdate();
        valueUpdate.setAction(AttributeAction.DELETE);
        item.put(key, valueUpdate);
    }
}
