require('./integration_config.js');

var request = require('request');
var http = require('http');
var webhookName = utils.randomChannelName();
var webhookResource = utils.getWebhookUrl() + "/" + webhookName;
var testName = __filename;

describe(testName, function () {

    it('gets missing webhook ' + webhookName, function (done) {
        request.get({
                url: webhookResource,
                headers : {"Content-Type" : "application/json"} },
            function (err, response, body) {
                expect(err).toBeNull();
                expect(response.statusCode).toBe(404);
                done();
            });
    });

});

