require('../integration_config');
const { getProp, fromObjectPath, hubClientGet } = require('../lib/helpers');
const channelResource = `${channelUrl}/${utils.randomChannelName()}`;
var webhookResource = `${utils.getWebhookUrl()}/${utils.randomChannelName()}`;

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

    it('determines if this is a single or clustered hub', async () => {
        const url = `${hubUrlBase}/internal/properties`;
        const response = await hubClientGet(url);
        expect(getProp('statusCode', response)).toEqual(200);
        const properties = fromObjectPath(['body', 'properties'], response) || {};
        const hubType = properties['hub.type'];
        if (hubType !== undefined) {
            isClustered = hubType === 'aws';
        }
        console.log('isClustered:', isClustered);
    });

    it('creates a channel', (done) => {
        utils.httpPut(channelResource)
            .then(response => expect(getProp('statusCode', response)).toEqual(201))
            .finally(done);
    });

    it('creates a webhook pointing at localhost', (done) => {
        let headers = {'Content-Type': 'application/json'};
        let body = {
            callbackUrl: 'http://localhost:8080/nothing',
            channelUrl: channelResource,
        };
        utils.httpPut(webhookResource, headers, body)
            .then(response => {
                const statusCode = getProp('statusCode', response);
                if (isClustered) {
                    expect(statusCode).toEqual(400);
                } else {
                    expect(statusCode).toEqual(201);
                }
            })
            .finally(done);
    });
});
