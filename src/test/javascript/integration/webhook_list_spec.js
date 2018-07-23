require('../integration_config');
const { getProp, fromObjectPath } = require('../lib/helpers');
var webhookName1 = utils.randomChannelName();
var webhookName2 = utils.randomChannelName();
var webhookUrl = utils.getWebhookUrl();
var testName = __filename;
var webhookConfig = {
    callbackUrl: 'http://nothing/callback',
    channelUrl: 'http://nothing/channel/notHere'
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
                const location = fromObjectPath(['headers', 'location'], response);
                const body = getProp('body', response) || {};
                expect(getProp('statusCode', response)).toEqual(201);
                expect(location).toBe(firstWebhookURL);
                expect(body.callbackUrl).toBe(webhookConfig.callbackUrl);
                expect(body.channelUrl).toBe(webhookConfig.channelUrl);
                expect(body.name).toBe(webhookName1);
            })
            .finally(done);
    });

    var secondWebhookURL = webhookUrl + '/' + webhookName2;

    it('creates the second webhook', function (done) {
        var url = secondWebhookURL;
        var headers = {'Content-Type': 'application/json'};
        var body = webhookConfig;

        utils.httpPut(url, headers, body)
            .then(function (response) {
                const location = fromObjectPath(['headers', 'location'], response);
                const body = getProp('body', response) || {};
                expect(getProp('statusCode', response)).toEqual(201);
                expect(location).toBe(secondWebhookURL);
                expect(body.callbackUrl).toBe(webhookConfig.callbackUrl);
                expect(body.channelUrl).toBe(webhookConfig.channelUrl);
                expect(body.name).toBe(webhookName2);
            })
            .finally(done);
    });

    var foundURLs = [];

    it('gets a list of the webhooks', function (done) {
        var url = webhookUrl;
        var headers = {'Content-Type': 'application/json'};

        utils.httpGet(url, headers)
            .then(function (response) {
                expect(getProp('statusCode', response)).toBe(200);
                const selfLink = fromObjectPath(['body', '_links', 'self', 'href'], response);
                const groups = fromObjectPath(['body', '_links', 'groups'], response);
                const webhooks = fromObjectPath(['body', '_links', 'webhooks'], response);
                expect(selfLink).toEqual(webhookUrl);
                foundURLs = (groups || webhooks)
                    .map(item => getProp('href', item))
                    .filter(href =>
                        [firstWebhookURL, secondWebhookURL]
                            .some(val =>
                                val === href));
            })
            .finally(done);
    });

    it('verifies we found the correct URLs', function () {
        expect(foundURLs.length).toEqual(2);
        expect(foundURLs).toContain(firstWebhookURL);
        expect(foundURLs).toContain(secondWebhookURL);
    });

    utils.deleteWebhook(webhookName1);
    utils.deleteWebhook(webhookName2);

});
