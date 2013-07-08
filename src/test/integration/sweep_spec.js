require('./integration_config.js');

var testName = 'sweep_spec';

// Shouldn't take this long if hub and cassandra are both in VPC, but running locally it can a very long time due the latency
// involved in each channel being checked for data.
utils.configureFrisby( 300000 );

frisby.create(testName + ': Manually invoke the sweeper.')
    .post(dataHubUrlBase + "/sweep", null, {})
    .expectStatus(200)
    .toss();




