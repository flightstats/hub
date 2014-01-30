require('./integration_config.js');

var jsonBody = JSON.stringify({
    historicalDays: 5,
    includeExcept: ["two", "one"]
});
var replicationResource = dataHubUrlBase + "/replication/includedDomain";
var testName = "replication_included_spec";
utils.configureFrisby();

frisby.create(testName + ': check for replication')
    .put(replicationResource, null, { body: jsonBody})
    .addHeader("Content-Type", "application/json")
    .expectStatus(201)
    .expectHeader('content-type', 'application/json')
    .expectHeader('location', replicationResource)
    .expectJSON({
        domain: "includedDomain",
        historicalDays: 5,
        includeExcept: ["one", "two"],
        excludeExcept: []
    })
    .afterJSON(function (result) {
        frisby.create(testName + ': Making sure replication exists.')
            .get(replicationResource)
            .expectStatus(200)
            .toss();
    })
    .toss();
