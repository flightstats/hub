require('./../integration/integration_config.js');

var jsonBody = JSON.stringify({
    historicalDays : 5,
    excludeExcept : ["two", "one", "3"]
});
var replicationResource = hubUrlBase + "/replication/excludedDomain";
var testName = "replication_excluded_spec";
utils.configureFrisby();

frisby.create(testName + ': check for replication')
    .put(replicationResource, null, { body : jsonBody})
    .addHeader("Content-Type", "application/json")
    .expectStatus(201)
    .expectHeader('content-type', 'application/json')
    .expectHeader('location', replicationResource)
    .expectJSON({
        domain : "excludedDomain",
        historicalDays : 5,
        excludeExcept : ["3", "one", "two"]
    })
    .afterJSON(function (result) {
        frisby.create(testName + ': Making sure replication exists.')
            .get(replicationResource)
            .expectStatus(200)
            .toss();
    })
    .toss();
