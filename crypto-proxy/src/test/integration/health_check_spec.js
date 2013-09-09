require('./integration_config.js');

frisby.create('Making sure channel resource does not yet exist.')
    .get(cyptoProxyUrlBase + "/health")
    .expectStatus(200)
    .expectHeader("content-type", "text/plain")
    .after(function (err, res, body) {
        expect(body).toMatch(/OK/);

    })
    .toss();



