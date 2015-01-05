require('./integration_config.js');

var jsonBody = JSON.stringify({
    historicalDays : 5,
    includeExcept : ["two", "one"],
    excludeExcept : ["3", "4"]
});
var replicationResource = hubUrlBase + "/replication/bothError";
var testName = "replication_both_error_spec";
utils.configureFrisby();

frisby.create(testName + ': check for replication')
    .put(replicationResource, null, { body : jsonBody})
    .addHeader("Content-Type", "application/json")
    .expectStatus(400)
    .toss();
