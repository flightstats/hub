require('../integration_config');
const {
    getProp,
    getWebhookUrl,
    fromObjectPath,
    hubClientGet,
    hubClientPut,
} = require('../lib/helpers');
const channelResource = `${channelUrl}/${utils.randomChannelName()}`;
const webhookResource = `${getWebhookUrl()}/${utils.randomChannelName()}`;

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
    const headers = { 'Content-Type': 'application/json' };
    it('determines if this is a single or clustered hub', async () => {
        const url = `${hubUrlBase}/internal/properties`;
        const response = await hubClientGet(url, headers);
        expect(getProp('statusCode', response)).toEqual(200);
        const properties = fromObjectPath(['body', 'properties'], response) || {};
        const hubType = properties['hub.type'];
        if (hubType !== undefined) {
            isClustered = hubType === 'aws';
        }
        console.log('isClustered:', isClustered);
    });

    it('creates a channel', async () => {
        const response = await hubClientPut(channelResource);
        expect(getProp('statusCode', response)).toEqual(201);
    });

    it('creates a webhook pointing at localhost', async () => {
        const body = {
            callbackUrl: 'http://localhost:8080/nothing',
            channelUrl: channelResource,
        };
        const response = await hubClientPut(webhookResource, headers, body);
        const statusCode = getProp('statusCode', response);
        const expected = isClustered ? 400 : 201;
        expect(statusCode).toEqual(expected);
    });
});
