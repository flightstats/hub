require('./integration_config.js');

var jsonBody = JSON.stringify({
    historicalDays : null,
    includeExcept : ["two", "one"]
});
var replicationResource = hubUrlBase + "/replication/nullHistorical";
var testName = "replication_historical_non_number_spec";
utils.configureFrisby();

frisby.create(testName + ': null historical')
    .put(replicationResource, null, { body : jsonBody})
    .addHeader("Content-Type", "application/json")
    .expectStatus(400)
    .toss();
