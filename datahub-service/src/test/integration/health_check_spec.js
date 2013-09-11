require('./integration_config.js');
var testName = 'health_check_spec';
frisby.create(testName + ': Making sure channel resource does not yet exist.')
    .get(dataHubUrlBase + "/health")
    .expectStatus(200)
    .expectHeader("content-type", "text/plain")
    .after(function (err, res, body) {
        expect(body).toMatch(/OK/);

    })
    .toss();



