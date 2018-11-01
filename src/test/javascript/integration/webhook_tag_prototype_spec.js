const {
    getProp,
    getWebhookUrl,
    hubClientDelete,
    hubClientGet,
    hubClientPut,
    isClusteredHubNode,
    itSleeps,
    randomChannelName,
    randomTag,
} = require('../lib/helpers');
const {
    getChannelUrl,
    getHubUrlBase,
} = require('../lib/config');
const channelUrl = getChannelUrl();

const tag = randomTag();
const tagURL = `${getHubUrlBase()}/tag/${tag}`;
const tagWebhookPrototypeURL = `${getWebhookUrl()}/TAGWHPROTO_${tag}`;
const channelOneName = randomChannelName();
const channelOneURL = `${channelUrl}/${channelOneName}`;
const channelOneWebhookURL = `${getWebhookUrl()}/TAGWH_${tag}_${channelOneName}`;
const channelTwoName = randomChannelName();
const channelTwoURL = `${channelUrl}/${channelTwoName}`;
const channelTwoWebhookURL = `${getWebhookUrl()}/TAGWH_${tag}_${channelTwoName}`;
const context = {
    [tag]: {
        isClustered: false,
    },
};
const acceptJSON = { "Content-Type": "application/json" };

describe(__filename, function () {
    beforeAll(async () => {
        context[tag].isClustered = await isClusteredHubNode();
        console.log('isClustered:', context[tag].isClustered);
    });

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

    it('waits 2000 ms', async () => {
        await itSleeps(2000);
    });

    it('verifies the webhook created for channel one is removed ** (if clustered hub)', async () => {
        /*
          singleton hub instances DO NOT successfully delete the webhook
          to the best that I can tell -pjh 08/24/18
        */
        const response = await hubClientGet(channelOneWebhookURL);
        const { isClustered } = context[tag];
        const expected = isClustered ? 404 : 200;
        expect(getProp('statusCode', response)).toEqual(expected);
    });

    it('removes the tag webhook prototype', async () => {
        const response = await hubClientDelete(tagWebhookPrototypeURL);
        expect(getProp('statusCode', response)).toEqual(202);
    });

    it('waits 1000 ms', async () => {
        await itSleeps(1000);
    });

    it('verifies the webhook created for channel two is removed ** (if clustered hub)', async () => {
        /*
          singleton hub instances DO NOT successfully delete the webhook
          to the best that I can tell -pjh 08/24/18
        */
        const response = await hubClientGet(channelTwoWebhookURL);
        const { isClustered } = context[tag];
        const expected = isClustered ? 404 : 200;
        expect(getProp('statusCode', response)).toEqual(expected);
    });

    afterAll(async () => {
        await hubClientDelete(channelOneURL);
        await hubClientDelete(channelTwoURL);
    });
});
