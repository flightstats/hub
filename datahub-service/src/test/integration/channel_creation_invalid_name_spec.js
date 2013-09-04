require('./integration_config.js');

utils.configureFrisby();

var jsonBody = JSON.stringify({ "name": "not valid!"});

frisby.create('Test create channel with invalid name')
    .post(channelUrl, null, { body: jsonBody })
    .addHeader("Content-Type", "application/json")
    .expectStatus(400)
    .toss();



