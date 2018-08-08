require('../integration_config');
const { getProp, hubClientGet } = require('../lib/helpers');
const tag = utils.randomTag();
const tagURL = `${hubUrlBase}/tag/${tag}`;
const tagWebhookPrototypeURL = `${utils.getWebhookUrl()}/TAGWHPROTO_${tag}`;

const channelOneName = utils.randomChannelName();
const channelOneURL = `${channelUrl}/${channelOneName}`;
const channelOneWebhookURL = `${utils.getWebhookUrl()}/TAGWH_${tag}_${channelOneName}`;

const channelTwoName = utils.randomChannelName();
const channelTwoURL = `${channelUrl}/${channelTwoName}`;
const channelTwoWebhookURL = `${utils.getWebhookUrl()}/TAGWH_${tag}_${channelTwoName}`;

const acceptJSON = { "Content-Type": "application/json" };

describe(__filename, function () {
    it('creates a tag webhook prototype', (done) => {
        const config = {
            "callbackUrl": "http://nothing/callback",
            "tagUrl": tagURL,
        };
        utils.httpPut(tagWebhookPrototypeURL, acceptJSON, config)
            .then(response => expect(getProp('statusCode', response)).toEqual(201))
            .finally(done);
    });

    it('verifies the tag webhook prototype exists', async () => {
        const response = await hubClientGet(tagWebhookPrototypeURL);
        expect(getProp('statusCode', response)).toEqual(200);
    });

    it(`creates channel one with tag ${tag}`, (done) => {
        const config = { "tags": [tag] };
        utils.httpPut(channelOneURL, acceptJSON, config)
            .then(response => expect(getProp('statusCode', response)).toEqual(201))
            .finally(done);
    });

    it(`creates channel two with tag ${tag}`, (done) => {
        const config = { "tags": [tag] };
        utils.httpPut(channelTwoURL, acceptJSON, config)
            .then(response => expect(getProp('statusCode', response)).toEqual(201))
            .finally(done);
    });

    utils.itSleeps(1000);

    it('verifies a webhook for channel one exists', async () => {
        const response = await hubClientGet(channelOneWebhookURL);
        expect(getProp('statusCode', response)).toEqual(200);
    });

    it('verifies a webhook for channel two exists', async () => {
        const response = await hubClientGet(channelTwoWebhookURL);
        expect(getProp('statusCode', response)).toEqual(200);
    });

    it('removes the tag from channel one', (done) => {
        const config = { "tags": [] };
        utils.httpPut(channelOneURL, acceptJSON, config)
            .then(response => expect(getProp('statusCode', response)).toEqual(201))
            .finally(done);
    });

    utils.itSleeps(1000);

    it('verifies the webhook created for channel one is removed', async () => {
        const response = await hubClientGet(channelOneWebhookURL);
        expect(getProp('statusCode', response)).toEqual(404);
    });

    it('removes the tag webhook prototype', (done) => {
        utils.httpDelete(tagWebhookPrototypeURL)
            .then(response => expect(getProp('statusCode', response)).toEqual(202))
            .finally(done);
    });

    utils.itSleeps(1000);

    it('verifies the webhook created for channel two is removed', async () => {
        const response = await hubClientGet(channelTwoWebhookURL);
        expect(getProp('statusCode', response)).toEqual(404);
    });
});
