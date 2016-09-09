require('./integration_config.js');

var request = require('request');
var http = require('http');
var webhookName1 = utils.randomChannelName();
var webhookName2 = utils.randomChannelName();
var webhookUrl = utils.getWebhookUrl();
var testName = __filename;
var webhookConfig = {
    callbackUrl : 'http://nothing/callback',
    channelUrl : 'http://nothing/channel/notHere'
};

/**
 * This should:
 *
 * 1 - create webhooks
 * 2 - make sure they exist
 */
describe(testName, function () {

    var webhookHrefs = [
        utils.putWebhook(webhookName1, webhookConfig, 201, testName, webhookUrl),
        utils.putWebhook(webhookName2, webhookConfig, 201, testName, webhookUrl)
    ];
    var foundWebhookHrefs = [];

    it('gets the webhooks ', function () {
        runs(function () {
            request.get({url: webhookUrl, headers: {'Content-Type': 'application/json'}},
                function (err, response, body) {
                    expect(err).toBeNull();
                    expect(response.statusCode).toBe(200);
                    var parse = utils.parseJson(response, testName);
                    expect(parse._links.self.href).toBe(webhookUrl);
                    var webhooks = parse._links.groups || parse._links.webhooks;
                    webhooks.forEach(function (item) {
                        if (item.name === webhookName1 || item.name === webhookName2) {
                            foundWebhookHrefs.push(item.href);
                        }
                    });
                });
        });

        waitsFor(function () {
            return foundWebhookHrefs.length === 2;
        });

        runs(function () {
            webhookHrefs.forEach(function (item) {
                expect(foundWebhookHrefs.indexOf(item)).not.toBe(-1);
            })
        });

    });

    utils.deleteWebhook(webhookName1);
    utils.deleteWebhook(webhookName2);

});

