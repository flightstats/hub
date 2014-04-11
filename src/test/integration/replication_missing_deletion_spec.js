require('./integration_config.js');

var replicationResource = hubUrlBase + "/replication/nonExistent";
var testName = "replication_missing_deletion_spec";
utils.configureFrisby();

frisby.create(testName + ': missing replication deletion')
    .delete(replicationResource)
    .addHeader("Content-Type", "application/json")
    .expectStatus(404)
    .toss();
