require('../integration_config');

var request = require('request');
var http = require('http');
var channelName = utils.randomChannelName();
var webhookName = utils.randomChannelName();
var channelResource = channelUrl + "/" + channelName;
var webhookResource = utils.getWebhookUrl() + "/" + webhookName;

/**
 * This should:
 *
 * 1 - create a channel
 * 2 - create a webhook pointing at localhost
 * 3a - webhook creation should fail with a clustered hub
 * 3b - webhook creation should succeed with a single hub
 */

describe(__filename, function () {

    let isClustered = true;

    it('determines if this is a single or clustered hub', (done) => {
        let url = `${hubUrlBase}/internal/properties`;
        utils.httpGet(url)
            .then(response => {
                expect(response.statusCode).toEqual(200);
                let hubType = response.body.properties['hub.type'];
                if (hubType !== undefined) {
                    isClustered = hubType === 'aws';
                }
                console.log('isClustered:', isClustered);
            })
            .finally(done);
    });

    it('creates a channel', (done) => {
        utils.httpPut(channelResource)
            .then(response => expect(response.statusCode).toEqual(201))
            .finally(done);
    });

    it('creates a webhook pointing at localhost', (done) => {
        let headers = {'Content-Type': 'application/json'};
        let body = {
            callbackUrl: 'http://localhost:8080/nothing',
            channelUrl: channelResource
        };
        utils.httpPut(webhookResource, headers, body)
            .then(response => {
                if (isClustered) {
                    expect(response.statusCode).toEqual(400)
                } else {
                    expect(response.statusCode).toEqual(201);
                }
            })
            .finally(done);
    });

});
