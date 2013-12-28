package com.flightstats.datahub.dao.dynamo;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.model.*;
import com.flightstats.datahub.dao.ContentDao;
import com.flightstats.datahub.dao.TimeIndexDates;
import com.flightstats.datahub.model.*;
import com.google.common.base.Optional;
import com.google.inject.Inject;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.*;

/**
 *
 */
public class DynamoTimeSeriesContentDao implements ContentDao {

    private final static Logger logger = LoggerFactory.getLogger(DynamoTimeSeriesContentDao.class);

    private final AmazonDynamoDBClient dbClient;
    private final DynamoUtils dynamoUtils;

    @Inject
    public DynamoTimeSeriesContentDao(AmazonDynamoDBClient dbClient,
                                      DynamoUtils dynamoUtils) {
        this.dbClient = dbClient;
        this.dynamoUtils = dynamoUtils;
    }

    @Override
    public ValueInsertionResult write(String channelName, Content content, Optional<Integer> ttlSeconds) {
        //todo - gfm - 12/11/13 - this may need to change if we don't want to create unlimited dynamo tables in dev & qa
        ContentKey key = new TimeSeriesContentKey();

        Map<String, AttributeValue> item = new HashMap<>();
        item.put("key", new AttributeValue().withS(key.keyToString()));
        item.put("data", new AttributeValue().withB(ByteBuffer.wrap(content.getData())));
        item.put("hashstamp", new AttributeValue().withS(TimeIndexDates.getString()));
        item.put("millis", new AttributeValue().withN(String.valueOf(content.getMillis())));
        if (content.getContentType().isPresent()) {
            item.put("contentType", new AttributeValue(content.getContentType().get()));
        }
        if (content.getContentLanguage().isPresent()) {
            item.put("contentLanguage", new AttributeValue(content.getContentLanguage().get()));
        }

        PutItemRequest putItemRequest = new PutItemRequest()
                .withTableName(dynamoUtils.getTableName(channelName))
                .withItem(item);
        //todo - gfm - 12/13/13 - this needs to handle ProvisionedThroughputExceededException
        //return a 503 to the client
        PutItemResult result = dbClient.putItem(putItemRequest);
        return new ValueInsertionResult(key, new Date(content.getMillis()));
    }

    @Override
    public Content read(String channelName, ContentKey key) {

        HashMap<String, AttributeValue> keyMap = new HashMap<>();
        keyMap.put("key", new AttributeValue().withS(key.keyToString()));

        GetItemRequest getItemRequest = new GetItemRequest()
                .withTableName(dynamoUtils.getTableName(channelName))
                .withKey(keyMap);

        GetItemResult result = dbClient.getItem(getItemRequest);
        if (result.getItem() == null) {
            return null;
        }
        Map<String, AttributeValue> item = result.getItem();
        Optional<String> contentType = Optional.absent();
        AttributeValue contentTypeAttribute = item.get("contentType");
        if (null != contentTypeAttribute) {
            contentType = Optional.of(contentTypeAttribute.getS());
        }
        Optional<String> contentLanguage = Optional.absent();
        AttributeValue contentLanguageAttribute = item.get("contentLanguage");
        if (null != contentLanguageAttribute) {
            contentLanguage = Optional.of(contentLanguageAttribute.getS());
        }
        long millis = Long.parseLong(item.get("millis").getN());
        ByteBuffer byteBuffer = item.get("data").getB();
        return new Content(contentType, contentLanguage, byteBuffer.array() , millis);
    }

    @Override
    public void initialize() {
        //do nothing
    }

    @Override
    public void initializeChannel(ChannelConfiguration config) {

        /**
         * The Primary Key should remain a hash key so items are directly retrievable.
         * To support range queries based on time, we may need some sort of Global Secondary Index
         * A - we could simply use a small time value (minute?) as a String as the GSI's hash key,
         * then be able to pull back groups of id's directly.
         * This time based grouping could cause issues with hot spots under high load.
         * B - Use a batching mechanism to allow retrieval based on time
         * This would not be unique to non-sequentials
         *
         */
        //todo - gfm - 12/21/13 - this is option A
        ArrayList<AttributeDefinition> attributeDefinitions = new ArrayList<>();

        long indexThroughput = config.getRequestRateInSeconds();
        attributeDefinitions.add(new AttributeDefinition().withAttributeName("hashstamp").withAttributeType("S"));
        GlobalSecondaryIndex secondaryIndex = new GlobalSecondaryIndex()
                .withIndexName("TimeIndex")
                .withProvisionedThroughput(new ProvisionedThroughput(indexThroughput, indexThroughput))
                .withProjection(new Projection().withProjectionType("KEYS_ONLY"));

        ArrayList<KeySchemaElement> indexKeySchema = new ArrayList<>();

        indexKeySchema.add(new KeySchemaElement().withAttributeName("hashstamp").withKeyType(KeyType.HASH));

        secondaryIndex.setKeySchema(indexKeySchema);
        attributeDefinitions.add(new AttributeDefinition().withAttributeName("key").withAttributeType("S"));

        ArrayList<KeySchemaElement> tableKeySchema = new ArrayList<>();
        tableKeySchema.add(new KeySchemaElement().withAttributeName("key").withKeyType(KeyType.HASH));

        long tableThroughput = config.getContentThroughputInSeconds();
        String tableName = dynamoUtils.getTableName(config.getName());
        CreateTableRequest createTableRequest = new CreateTableRequest()
                .withTableName(tableName)
                .withAttributeDefinitions(attributeDefinitions)
                .withKeySchema(tableKeySchema)
                .withGlobalSecondaryIndexes(secondaryIndex)
                .withProvisionedThroughput(new ProvisionedThroughput(tableThroughput, tableThroughput));

        logger.info("creating times series " + tableName + " table with " + tableThroughput + " " + indexThroughput);
        dynamoUtils.createTable(createTableRequest);
    }

    @Override
    public Optional<ContentKey> getKey(String id) {
        return TimeSeriesContentKey.fromString(id);
    }

    @Override
    public Iterable<ContentKey> getKeys(String channelName, DateTime dateTime) {
        //todo see if Parallel Scan is relevant here - http://docs.aws.amazon.com/amazondynamodb/latest/developerguide/QueryAndScan.html
        List<ContentKey> keys = new ArrayList<>();
        QueryRequest queryRequest = getQueryRequest(channelName, dateTime);
        QueryResult result = dbClient.query(queryRequest);
        addResults(keys, result);
        while (result.getLastEvaluatedKey() != null) {
            queryRequest.setExclusiveStartKey(result.getLastEvaluatedKey());
            result = dbClient.query(queryRequest);
            addResults(keys, result);

        }
        logger.info("returning " + keys.size() + " keys for " + channelName + " at " + TimeIndexDates.getString(dateTime));
        return keys;
    }

    @Override
    public void delete(String channelName) {
        dbClient.deleteTable(dynamoUtils.getTableName(channelName));
    }

    private void addResults(List<ContentKey> keys, QueryResult result) {
        for (Map<String, AttributeValue> attribs : result.getItems()) {
            AttributeValue keyValue = attribs.get("key");
            if (keyValue != null) {
                Optional<ContentKey> keyOptional = TimeSeriesContentKey.fromString(keyValue.getS());
                if (keyOptional.isPresent()) {
                    keys.add(keyOptional.get());
                }
            }
        }
    }

    private QueryRequest getQueryRequest(String channelName, DateTime dateTime) {
        QueryRequest queryRequest = new QueryRequest()
                .withTableName(dynamoUtils.getTableName(channelName))
                .withIndexName("TimeIndex")
                .withSelect("ALL_PROJECTED_ATTRIBUTES")
                .withScanIndexForward(true);

        HashMap<String, Condition> keyConditions = new HashMap<>();

        keyConditions.put("hashstamp", new Condition()
                .withComparisonOperator(ComparisonOperator.EQ)
                .withAttributeValueList(new AttributeValue().withS(TimeIndexDates.getString(dateTime))));

        queryRequest.setKeyConditions(keyConditions);
        return queryRequest;
    }
}
