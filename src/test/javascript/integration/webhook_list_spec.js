require('../integration_config');
const { getProp, fromObjectPath, hubClientGet } = require('../lib/helpers');
const webhookName1 = utils.randomChannelName();
const webhookName2 = utils.randomChannelName();
const webhookUrl = utils.getWebhookUrl();
const webhookConfig = {
    callbackUrl: 'http://nothing/callback',
    channelUrl: 'http://nothing/channel/notHere',
};
/**
 * This should:
 *
 * 1 - create webhooks
 * 2 - make sure they exist
 */
const headers = { 'Content-Type': 'application/json' };
describe(__filename, function () {
    const firstWebhookURL = `${webhookUrl}/${webhookName1}`;

    it('creates the first webhook', function (done) {
        utils.httpPut(firstWebhookURL, headers, webhookConfig)
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
        utils.httpPut(secondWebhookURL, headers, webhookConfig)
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

    let foundURLs = [];

    it('gets a list of the webhooks', async () => {
        const response = await hubClientGet(webhookUrl, headers);
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
    });

    it('verifies we found the correct URLs', function () {
        expect(foundURLs.length).toEqual(2);
        expect(foundURLs).toContain(firstWebhookURL);
        expect(foundURLs).toContain(secondWebhookURL);
    });

    utils.deleteWebhook(webhookName1);
    utils.deleteWebhook(webhookName2);
});
