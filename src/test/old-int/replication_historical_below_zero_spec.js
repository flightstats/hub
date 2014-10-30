require('./integration_config.js');

var jsonBody = JSON.stringify({
    historicalDays : -1,
    includeExcept : ["two", "one"]
});
var replicationResource = hubUrlBase + "/replication/includedDomain";
var testName = "replication_historical_beow_zero_spec";
utils.configureFrisby();

frisby.create(testName + ': null historical')
    .put(replicationResource, null, { body : jsonBody})
    .addHeader("Content-Type", "application/json")
    .expectStatus(400)
    .toss();
