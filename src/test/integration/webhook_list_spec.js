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

    var firstWebhookURL = webhookUrl + '/' + webhookName1;
    
    it('creates the first webhook', function (done) {
        var url = firstWebhookURL;
        var headers = {'Content-Type': 'application/json'};
        var body = webhookConfig;
        
        utils.httpPut(url, headers, body)
            .then(function (response) {
                expect(response.statusCode).toEqual(201);
                expect(response.headers.location).toBe(firstWebhookURL);
                expect(response.body.callbackUrl).toBe(webhookConfig.callbackUrl);
                expect(response.body.channelUrl).toBe(webhookConfig.channelUrl);
                expect(response.body.name).toBe(webhookName1);
            })
            .catch(function (error) {
                expect(error).toBeNull();
            })
            .fin(function () {
                done();
            });
    });
    
    var secondWebhookURL = webhookUrl + '/' + webhookName2;
    
    it('creates the second webhook', function (done) {
        var url = secondWebhookURL;
        var headers = {'Content-Type': 'application/json'};
        var body = webhookConfig;

        utils.httpPut(url, headers, body)
            .then(function (response) {
                expect(response.statusCode).toEqual(201);
                expect(response.headers.location).toBe(secondWebhookURL);
                expect(response.body.callbackUrl).toBe(webhookConfig.callbackUrl);
                expect(response.body.channelUrl).toBe(webhookConfig.channelUrl);
                expect(response.body.name).toBe(webhookName2);
            })
            .catch(function (error) {
                expect(error).toBeNull();
            })
            .fin(function () {
                done();
            });
    });
    
    var foundWebhookHrefs = [];

    it('gets a list of the webhooks', function (done) {
        var url = webhookUrl;
        var headers = {'Content-Type': 'application/json'};

        utils.httpGet(url, headers)
            .then(function (response) {
                expect(response.statusCode).toBe(200);
                expect(response.body._links.self.href).toBe(webhookUrl);
                var items = response.body._links.groups || response.body._links.webhooks;
                for (var i = 0; i < items.length; ++i) {
                    if (item.name === webhookName1 || item.name === webhookName2) {
                        foundWebhookHrefs.push(item.href);
                    }
                }
                expect(foundWebhookHrefs.length).toEqual(2);
                expect(foundWebhookHrefs).toContain(firstWebhookURL);
                expect(foundWebhookHrefs).toContain(secondWebhookURL);
            })
            .catch(function (error) {
                expect(error).toBeNull();
            })
            .fin(function () {
                done();
            });
    });

    utils.deleteWebhook(webhookName1);
    utils.deleteWebhook(webhookName2);

});

