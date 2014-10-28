require('./../integration/integration_config.js');


var replicationResource = hubUrlBase + "/replication/not_there";
var testName = "replication_status_missing_spec";
utils.configureFrisby();

frisby.create(testName + ': getting status of missing domain')
    .get(replicationResource)
    .addHeader("Accepts", "application/json")
    .expectStatus(404)
    .toss();
