const { getProp, hubClientDelete, hubClientPut, randomChannelName } = require('../lib/helpers');
const request = require('request');
const {
    getChannelUrl,
} = require('../lib/config');

const channelUrl = getChannelUrl();
const channelName = randomChannelName();
const channelResource = `${channelUrl}/${channelName}`;
const headers = { 'Content-Type': 'application/json' };

describe(__filename, function () {
    beforeAll(async () => {
        const response = await hubClientPut(channelResource, headers, { name: channelName, ttlDays: 1 });
        expect(getProp('statusCode', response)).toEqual(201);
    });

    it(`gets latest ${__filename}`, function (done) {
        request.get({
            url: `${channelResource}/latest?stable=false`,
        },
        function (err, response, body) {
            expect(err).toBeNull();
            expect(getProp('statusCode', response)).toBe(404);
            done();
        });
    }, 2 * 60001);

    afterAll(async () => {
        await hubClientDelete(channelResource);
    });
});
