require('./integration_config.js');

var jsonBody = JSON.stringify({
    historicalDays : "blah",
    includeExcept : ["two", "one"]
});
var replicationResource = hubUrlBase + "/replication/nonNumber";
var testName = "replication_historical_non_number_spec";
utils.configureFrisby();

frisby.create(testName + ': non number historical')
    .put(replicationResource, null, { body : jsonBody})
    .addHeader("Content-Type", "application/json")
    .expectStatus(400)
    .toss();
