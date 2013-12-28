package dynamo;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.Protocol;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.model.*;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 *
 */
public class DynamoLocalError {

    //change these parameters for your system
    static String CREDENTIALS = "/Users/gmoulliet/code/datahub/datahub-service/src/conf/datahub/dev/credentials.properties";
    static String TABLE_NAME = "deihub_dev_DynamoLocalError";

    //works with this endpoint
    //static String endpoint="dynamodb.us-east-1.amazonaws.com";
    //fails with this enpoint running dynamodb_local_2013-12-12
    static String endpoint="localhost:8000";

    static AmazonDynamoDBClient dbClient;

    public static void main(String[] args) throws Exception {

        AWSCredentials awsCredentials = new PropertiesCredentials(new File(CREDENTIALS));
        dbClient = new AmazonDynamoDBClient(awsCredentials);
        ClientConfiguration configuration = new ClientConfiguration();
        configuration.setProtocol(Protocol.HTTP);
        dbClient.setConfiguration(configuration);
        dbClient.setEndpoint(endpoint);

        DynamoLocalError dynamoLocalError = new DynamoLocalError();
        dynamoLocalError.initializeTable();
        String indexKey = "myIndexKey";
        dynamoLocalError.write("this is some data", indexKey);
        QueryResult keys = dynamoLocalError.getKeys(indexKey);
        System.out.println("found keys " + keys);
    }

    public DynamoLocalError() {
    }

    public QueryResult getKeys(String indexKey) {
        QueryRequest queryRequest = new QueryRequest()
                .withTableName(TABLE_NAME)
                .withIndexName("Index")
                .withSelect("ALL_PROJECTED_ATTRIBUTES")
                .withScanIndexForward(true);

        HashMap<String, Condition> keyConditions = new HashMap<>();

        keyConditions.put("indexKey", new Condition()
                .withComparisonOperator(ComparisonOperator.EQ)
                .withAttributeValueList(new AttributeValue().withS(indexKey)));

        queryRequest.setKeyConditions(keyConditions);
        return dbClient.query(queryRequest);
    }

    public PutItemResult write(String data, String indexKey) {
        String key = UUID.randomUUID().toString();

        Map<String, AttributeValue> item = new HashMap<>();
        item.put("key", new AttributeValue().withS(key));
        item.put("data", new AttributeValue().withS(data));
        item.put("indexKey", new AttributeValue().withS(indexKey));

        PutItemRequest putItemRequest = new PutItemRequest()
                .withTableName(TABLE_NAME)
                .withItem(item);
        return dbClient.putItem(putItemRequest);
    }


    public void initializeTable() {

        ArrayList<AttributeDefinition> attributeDefinitions = new ArrayList<>();
        attributeDefinitions.add(new AttributeDefinition().withAttributeName("indexKey").withAttributeType("S"));
        GlobalSecondaryIndex secondaryIndex = new GlobalSecondaryIndex()
                .withIndexName("Index")
                .withProvisionedThroughput(new ProvisionedThroughput(10L, 10L))
                .withProjection(new Projection().withProjectionType("KEYS_ONLY"));

        ArrayList<KeySchemaElement> indexKeySchema = new ArrayList<>();

        indexKeySchema.add(new KeySchemaElement().withAttributeName("indexKey").withKeyType(KeyType.HASH));

        secondaryIndex.setKeySchema(indexKeySchema);
        attributeDefinitions.add(new AttributeDefinition().withAttributeName("key").withAttributeType("S"));

        ArrayList<KeySchemaElement> tableKeySchema = new ArrayList<>();
        tableKeySchema.add(new KeySchemaElement().withAttributeName("key").withKeyType(KeyType.HASH));

        CreateTableRequest request = new CreateTableRequest()
                .withTableName(TABLE_NAME)
                .withAttributeDefinitions(attributeDefinitions)
                .withKeySchema(tableKeySchema)
                .withGlobalSecondaryIndexes(secondaryIndex)
                .withProvisionedThroughput(new ProvisionedThroughput(10L, 10L));

        try {
            System.out.println(dbClient.describeTable(request.getTableName()).toString());
        } catch (ResourceNotFoundException e) {
            dbClient.createTable(request);
        }

    }

}
