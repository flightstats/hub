package com.flightstats.datahub.dao.dynamo;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.model.*;
import com.flightstats.datahub.dao.ContentDao;
import com.flightstats.datahub.model.ChannelConfiguration;
import com.flightstats.datahub.model.DataHubCompositeValue;
import com.flightstats.datahub.model.DataHubKey;
import com.flightstats.datahub.model.ValueInsertionResult;
import com.flightstats.datahub.util.DataHubKeyGenerator;
import com.flightstats.datahub.util.TimeProvider;
import com.google.common.base.Optional;
import com.google.inject.Inject;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 *
 */
public class DynamoTimeSeriesContentDao implements ContentDao {

    private final static Logger logger = LoggerFactory.getLogger(DynamoTimeSeriesContentDao.class);

    private final DataHubKeyGenerator keyGenerator;
    private final TimeProvider timeProvider;
    private final AmazonDynamoDBClient dbClient;
    private final DynamoUtils dynamoUtils;
    private final DateTimeFormatter formatter;
    private final DateTimeZone timeZone;

    @Inject
    public DynamoTimeSeriesContentDao(DataHubKeyGenerator keyGenerator,
                                      TimeProvider timeProvider,
                                      AmazonDynamoDBClient dbClient,
                                      DynamoUtils dynamoUtils) {
        this.keyGenerator = keyGenerator;
        this.timeProvider = timeProvider;
        this.dbClient = dbClient;
        this.dynamoUtils = dynamoUtils;
        formatter = DateTimeFormat.forPattern("yyyy-MM-dd-HH-mmZ");
        timeZone = DateTimeZone.forID("America/Los_Angeles");
    }

    @Override
    public ValueInsertionResult write(String channelName, DataHubCompositeValue value, Optional<Integer> ttlSeconds) {
        //todo - gfm - 12/11/13 - this may need to change if we don't want to create unlimited dynamo tables in dev & qa
        DataHubKey key = keyGenerator.newKey(channelName);

        Map<String, AttributeValue> item = new HashMap<>();
        item.put("key", new AttributeValue().withS(key.keyToString()));
        item.put("data", new AttributeValue().withB(ByteBuffer.wrap(value.getData())));
        item.put("hashstamp", new AttributeValue().withS(formatter.print(new DateTime().withZone(timeZone))));
        item.put("millis", new AttributeValue().withN(String.valueOf(value.getMillis())));
        if (value.getContentType().isPresent()) {
            item.put("contentType", new AttributeValue(value.getContentType().get()));
        }
        if (value.getContentLanguage().isPresent()) {
            item.put("contentLanguage", new AttributeValue(value.getContentLanguage().get()));
        }

        PutItemRequest putItemRequest = new PutItemRequest()
                .withTableName(dynamoUtils.getTableName(channelName))
                .withItem(item);
        //todo - gfm - 12/13/13 - this needs to handle ProvisionedThroughputExceededException
        PutItemResult result = dbClient.putItem(putItemRequest);
        //todo - gfm - 12/11/13 - do we need a rowkey for this?
        return new ValueInsertionResult(key, "", timeProvider.getDate());
    }

    @Override
    public DataHubCompositeValue read(String channelName, DataHubKey key) {

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
        return new DataHubCompositeValue(contentType, contentLanguage, byteBuffer.array() , millis);
    }

    @Override
    public void initialize() {
        //do nothing
    }

    @Override
    public void initializeChannel(ChannelConfiguration configuration) {

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

        attributeDefinitions.add(new AttributeDefinition()
                .withAttributeName("hashstamp")
                .withAttributeType("S"));
        attributeDefinitions.add(new AttributeDefinition()
                .withAttributeName("millis")
                .withAttributeType("N"));

        GlobalSecondaryIndex secondaryIndex = new GlobalSecondaryIndex()
                .withIndexName("TimeIndex")
                .withProvisionedThroughput(new ProvisionedThroughput(10L, 10L))
                .withProjection(new Projection().withProjectionType("KEYS_ONLY"));

        ArrayList<KeySchemaElement> indexKeySchema = new ArrayList<>();

        indexKeySchema.add(new KeySchemaElement()
                .withAttributeName("hashstamp")
                .withKeyType(KeyType.HASH));
        indexKeySchema.add(new KeySchemaElement()
                .withAttributeName("millis")
                .withKeyType(KeyType.RANGE));

        secondaryIndex.setKeySchema(indexKeySchema);

        attributeDefinitions.add(new AttributeDefinition().withAttributeName("key").withAttributeType("N"));

        ArrayList<KeySchemaElement> tableKeySchema = new ArrayList<>();
        tableKeySchema.add(new KeySchemaElement().withAttributeName("key").withKeyType(KeyType.HASH));

        CreateTableRequest createTableRequest = new CreateTableRequest()
                .withTableName(dynamoUtils.getTableName(configuration.getName()))
                .withAttributeDefinitions(attributeDefinitions)
                .withKeySchema(tableKeySchema)
                .withGlobalSecondaryIndexes(secondaryIndex)
                .withProvisionedThroughput(new ProvisionedThroughput(10L, 10L));

        keyGenerator.seedChannel(configuration.getName());
        dynamoUtils.createTable(createTableRequest);
    }

    @Override
    public Optional<DataHubKey> getKey(String id) {
        //todo - gfm - 12/21/13 -
        return Optional.absent();
    }

    @Override
    public Optional<Iterable<DataHubKey>> getKeys(String channelName, DateTime dateTime) {
        QueryRequest queryRequest = new QueryRequest()
                .withTableName(dynamoUtils.getTableName(channelName))
                .withIndexName("TimeIndex")
                .withSelect("ALL_PROJECTED_ATTRIBUTES")
                .withScanIndexForward(true);

        HashMap<String, Condition> keyConditions = new HashMap<>();

        keyConditions.put("hashstamp",new Condition()
                .withComparisonOperator(ComparisonOperator.EQ)
                .withAttributeValueList(new AttributeValue().withS(formatter.print(dateTime))));

        queryRequest.setKeyConditions(keyConditions);

        QueryResult result = dbClient.query(queryRequest);
        //todo - gfm - 12/21/13 - can this return null?
        for (Map<String, AttributeValue> attribs : result.getItems()) {

            for (Map.Entry<String, AttributeValue> attrib : attribs.entrySet()) {
                System.out.println(attrib.getKey() + " ---> " + attrib.getValue());
            }

            System.out.println();
        }
        return null;
    }
}
