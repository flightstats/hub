require('../integration_config');
const moment = require('moment');
const {
    fromObjectPath,
    getProp,
    hubClientPut,
    hubClientPostTestItem,
} = require('../lib/helpers');

const request = require('request');
const channelName = utils.randomChannelName();
const channelResource = `${channelUrl}/${channelName}`;
const headers = { 'Content-Type': 'application/json' };

/**
 * create a channel
 * post two items
 * stream both items back with bulk
 */
describe(__filename, function () {
    beforeAll(async () => {
        const response = await hubClientPut(channelResource, headers, { name: channelName, ttlDays: 1 });
        expect(getProp('statusCode', response)).toEqual(201);
    });

    it('posts an item successfully to a channel', async () => {
        const response = await hubClientPostTestItem(channelResource);
        expect(getProp('statusCode', response)).toEqual(201);
    });

    it('posts a second item successfully to a channel', async () => {
        const response = await hubClientPostTestItem(channelResource);
        expect(getProp('statusCode', response)).toEqual(201);
    });

    it("gets multipart items ", function (done) {
        request.get({
            url: channelResource + '/latest/10?stable=false&batch=true',
            followRedirect: false,
            headers: { Accept: "multipart/mixed" },
        }, function (err, response, body) {
            expect(err).toBeNull();
            expect(getProp('statusCode', response)).toBe(200);
            expect(fromObjectPath(['headers', 'content-type'], response)).toContain('multipart/mixed');
            const responseBody = getProp('body', response);
            expect(responseBody).toContain('\\"data\\"');
            /* eslint-disable no-useless-escape */
            const bodyMatch = responseBody.match(/(?![\\\"data\\\":])(\d*)(?=})/gm);
            /* eslint-enable no-useless-escape */
            const firstMatch = bodyMatch && bodyMatch[0];
            expect(firstMatch).toBeDefined();
            done();
        });
    });
});
