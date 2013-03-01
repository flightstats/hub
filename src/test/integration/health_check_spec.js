require('./integration_config.js');
var frisby = require('frisby');

frisby.create('Making sure channel resource does not yet exist.')
    .get(dataHubUrlBase + "/health")
    .expectStatus(200)
    .expectHeader("content-type", "text/plain")
    .after(function (err, res, body) {
        expect(body).toMatch(/OK \(\d+ channels\)/);

    })
    .toss();



