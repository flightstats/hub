package com.flightstats.datahub.dao.dynamo;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.model.*;
import com.flightstats.datahub.dao.ContentDao;
import com.flightstats.datahub.dao.timeIndex.TimeIndex;
import com.flightstats.datahub.model.ChannelConfiguration;
import com.flightstats.datahub.model.Content;
import com.flightstats.datahub.model.ContentKey;
import com.flightstats.datahub.model.ValueInsertionResult;
import com.flightstats.datahub.util.DataHubKeyGenerator;
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
public class DynamoContentDao implements ContentDao {

    private final static Logger logger = LoggerFactory.getLogger(DynamoContentDao.class);
    private static final String TIME_INDEX = "TimeIndex";
    private static final String KEY = "key";
    private static final String HASHSTAMP = "hashstamp";

    private final DataHubKeyGenerator keyGenerator;
    private final AmazonDynamoDBClient dbClient;
    private final DynamoUtils dynamoUtils;

    @Inject
    public DynamoContentDao(DataHubKeyGenerator keyGenerator,
                            AmazonDynamoDBClient dbClient,
                            DynamoUtils dynamoUtils) {
        this.keyGenerator = keyGenerator;
        this.dbClient = dbClient;
        this.dynamoUtils = dynamoUtils;
    }

    @Override
    public ValueInsertionResult write(String channelName, Content content, long ttlDays) {
        ContentKey key = keyGenerator.newKey(channelName);

        Map<String, AttributeValue> item = new HashMap<>();
        item.put(KEY, new AttributeValue(key.keyToString()));
        item.put("data", new AttributeValue().withB(ByteBuffer.wrap(content.getData())));
        DateTime dateTime = new DateTime(content.getMillis());
        item.put(HASHSTAMP, new AttributeValue(TimeIndex.getHash(dateTime)));
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
        dbClient.putItem(putItemRequest);
        return new ValueInsertionResult(key, dateTime.toDate());
    }

    @Override
    public Content read(String channelName, ContentKey key) {

        HashMap<String, AttributeValue> keyMap = new HashMap<>();
        keyMap.put(KEY, new AttributeValue().withS(key.keyToString()));

        GetItemRequest getItemRequest = new GetItemRequest()
                .withTableName(dynamoUtils.getTableName(channelName))
                .withKey(keyMap);

        GetItemResult result = dbClient.getItem(getItemRequest);
        if (result.getItem() == null) {
            return null;
        }
        Map<String, AttributeValue> item = result.getItem();
        Content.Builder builder = Content.builder();
        AttributeValue contentTypeAttribute = item.get("contentType");
        if (null != contentTypeAttribute) {
            builder.withContentType(contentTypeAttribute.getS());
        }
        AttributeValue contentLanguageAttribute = item.get("contentLanguage");
        if (null != contentLanguageAttribute) {
            builder.withContentLanguage(contentLanguageAttribute.getS());
        }
        long millis = Long.parseLong(item.get("millis").getN());
        ByteBuffer byteBuffer = item.get("data").getB();
        builder.withData(byteBuffer.array()).withMillis(millis);
        return builder.build();
    }

    @Override
    public void initialize() {
        //do nothing
    }

    @Override
    public void initializeChannel(ChannelConfiguration config) {

        long indexThroughput = config.getPeakRequestRateSeconds();

        GlobalSecondaryIndex secondaryIndex = new GlobalSecondaryIndex()
                .withIndexName(TIME_INDEX)
                .withProvisionedThroughput(new ProvisionedThroughput(indexThroughput, indexThroughput))
                .withProjection(new Projection().withProjectionType(ProjectionType.KEYS_ONLY))
                .withKeySchema(new KeySchemaElement(HASHSTAMP, KeyType.HASH));

        long tableThroughput = config.getContentThroughputInSeconds();
        String tableName = dynamoUtils.getTableName(config.getName());

        CreateTableRequest createTableRequest = new CreateTableRequest()
                .withTableName(tableName)
                .withKeySchema(new KeySchemaElement(KEY, KeyType.HASH))
                .withProvisionedThroughput(new ProvisionedThroughput(tableThroughput, tableThroughput))
                .withGlobalSecondaryIndexes(secondaryIndex)
                .withAttributeDefinitions(
                        new AttributeDefinition(KEY, ScalarAttributeType.S),
                        new AttributeDefinition(HASHSTAMP, ScalarAttributeType.S)
                );

        logger.info("creating table " + tableName + " table with " + tableThroughput + " " + indexThroughput);
        keyGenerator.seedChannel(config.getName());
        dynamoUtils.createTable(createTableRequest);
    }

    @Override
    public Optional<ContentKey> getKey(String id) {
        return keyGenerator.parse(id);
    }

    @Override
    public Collection<ContentKey> getKeys(String channelName, DateTime dateTime) {
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
        logger.info("returning " + keys.size() + " keys for " + channelName + " at " + TimeIndex.getHash(dateTime));
        return keys;
    }

    @Override
    public void delete(String channelName) {
        dynamoUtils.deleteChannel(channelName);
    }

    @Override
    public void updateChannel(ChannelConfiguration configuration) {
        boolean update = false;
        String tableName = dynamoUtils.getTableName(configuration.getName());
        TableDescription table = dbClient.describeTable(tableName).getTable();

        UpdateTableRequest updateTableRequest = new UpdateTableRequest().withTableName(tableName);
        long tableThroughput = configuration.getContentThroughputInSeconds();
        if (table.getProvisionedThroughput().getWriteCapacityUnits() != tableThroughput) {
            update = true;
            updateTableRequest.withProvisionedThroughput(new ProvisionedThroughput(tableThroughput, tableThroughput));
        }

        List<GlobalSecondaryIndexDescription> gsis = table.getGlobalSecondaryIndexes();
        for (GlobalSecondaryIndexDescription gsi : gsis) {
            if (gsi.getIndexName().equals(TIME_INDEX)) {
                long indexThroughput = configuration.getPeakRequestRateSeconds();
                if (gsi.getProvisionedThroughput().getWriteCapacityUnits() != indexThroughput) {
                    update = true;
                    UpdateGlobalSecondaryIndexAction indexAction = new UpdateGlobalSecondaryIndexAction()
                            .withIndexName(TIME_INDEX)
                            .withProvisionedThroughput(new ProvisionedThroughput(indexThroughput, indexThroughput));

                    updateTableRequest.withGlobalSecondaryIndexUpdates(new GlobalSecondaryIndexUpdate().withUpdate(indexAction));
                }
            }
        }
        if (update) {
            dbClient.updateTable(updateTableRequest);
        }
    }

    private void addResults(List<ContentKey> keys, QueryResult result) {
        for (Map<String, AttributeValue> attribs : result.getItems()) {
            AttributeValue keyValue = attribs.get(KEY);
            if (keyValue != null) {
                Optional<ContentKey> keyOptional = getKey(keyValue.getS());
                if (keyOptional.isPresent()) {
                    keys.add(keyOptional.get());
                }
            }
        }
    }

    private QueryRequest getQueryRequest(String channelName, DateTime dateTime) {
        QueryRequest queryRequest = new QueryRequest()
                .withTableName(dynamoUtils.getTableName(channelName))
                .withIndexName(TIME_INDEX)
                .withConsistentRead(false)
                .withSelect("ALL_PROJECTED_ATTRIBUTES")
                .withScanIndexForward(true);

        HashMap<String, Condition> keyConditions = new HashMap<>();

        keyConditions.put(HASHSTAMP, new Condition()
                .withComparisonOperator(ComparisonOperator.EQ)
                .withAttributeValueList(new AttributeValue(TimeIndex.getHash(dateTime))));

        queryRequest.setKeyConditions(keyConditions);
        return queryRequest;
    }
}
