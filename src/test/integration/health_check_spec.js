require('./integration_config.js');

var testName = 'health_check_spec';

frisby.create(testName + ': Making sure channel resource does not yet exist.')
    .get(hubUrlBase + "/health")
    .expectStatus(200)
    .expectHeader('content-type', 'application/json')
    .expectJSON({
        healthy: true
    })
    .toss();



