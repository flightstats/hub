require('../integration_config');
const {
    getProp,
    getWebhookUrl,
    hubClientGet,
    hubClientPut,
    hubClientDelete,
    itSleeps,
} = require('../lib/helpers');
const tag = utils.randomTag();
const tagURL = `${hubUrlBase}/tag/${tag}`;
const tagWebhookPrototypeURL = `${getWebhookUrl()}/TAGWHPROTO_${tag}`;

const channelOneName = utils.randomChannelName();
const channelOneURL = `${channelUrl}/${channelOneName}`;
const channelOneWebhookURL = `${getWebhookUrl()}/TAGWH_${tag}_${channelOneName}`;

const channelTwoName = utils.randomChannelName();
const channelTwoURL = `${channelUrl}/${channelTwoName}`;
const channelTwoWebhookURL = `${getWebhookUrl()}/TAGWH_${tag}_${channelTwoName}`;

const acceptJSON = { "Content-Type": "application/json" };

describe(__filename, function () {
    it('creates a tag webhook prototype', async () => {
        const config = {
            "callbackUrl": "http://nothing/callback",
            "tagUrl": tagURL,
        };
        const response = await hubClientPut(tagWebhookPrototypeURL, acceptJSON, config);
        expect(getProp('statusCode', response)).toEqual(201);
    });

    it('verifies the tag webhook prototype exists', async () => {
        const response = await hubClientGet(tagWebhookPrototypeURL);
        expect(getProp('statusCode', response)).toEqual(200);
    });

    it(`creates channel one with tag ${tag}`, async () => {
        const config = { "tags": [tag] };
        const response = await hubClientPut(channelOneURL, acceptJSON, config);
        expect(getProp('statusCode', response)).toEqual(201);
    });

    it(`creates channel two with tag ${tag}`, async () => {
        const config = { "tags": [tag] };
        const response = await hubClientPut(channelTwoURL, acceptJSON, config);
        expect(getProp('statusCode', response)).toEqual(201);
    });

    it('waits 1000 ms', async () => {
        await itSleeps(1000);
    });

    it('verifies a webhook for channel one exists', async () => {
        const response = await hubClientGet(channelOneWebhookURL);
        expect(getProp('statusCode', response)).toEqual(200);
    });

    it('verifies a webhook for channel two exists', async () => {
        const response = await hubClientGet(channelTwoWebhookURL);
        expect(getProp('statusCode', response)).toEqual(200);
    });

    it('removes the tag from channel one', async () => {
        const config = { "tags": [] };
        const response = await hubClientPut(channelOneURL, acceptJSON, config);
        expect(getProp('statusCode', response)).toEqual(201);
    });

    it('waits 1000 ms', async () => {
        await itSleeps(1000);
    });

    it('verifies the webhook created for channel one is removed', async () => {
        const response = await hubClientGet(channelOneWebhookURL);
        expect(getProp('statusCode', response)).toEqual(404);
    });

    it('removes the tag webhook prototype', async () => {
        const response = await hubClientDelete(tagWebhookPrototypeURL);
        expect(getProp('statusCode', response)).toEqual(202);
    });

    it('waits 1000 ms', async () => {
        await itSleeps(1000);
    });

    it('verifies the webhook created for channel two is removed', async () => {
        const response = await hubClientGet(channelTwoWebhookURL);
        expect(getProp('statusCode', response)).toEqual(404);
    });
});
