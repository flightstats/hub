const { getProp, parseJson, randomChannelName } = require('../lib/helpers');
const {
    getChannelUrl,
} = require('../lib/config');

const channelUrl = getChannelUrl();
const request = require('request');
const channelName = randomChannelName();
const channelResource = `${channelUrl}/${channelName}`;

describe(__filename, function () {
    it(`fails to create channel ${channelName} at ${channelUrl}`, function (done) {
        request.post({url: channelUrl,
            headers: {"Content-Type": "application/json"},
            body: JSON.stringify({
                name: channelName,
                tags: ['one', 'tw*o'],
            })},
        function (err, response, body) {
            expect(err).toBeNull();
            expect(getProp('statusCode', response)).toBe(400);
            done();
        });
    });

    it(`creates channel ${channelName} at ${channelUrl}`, function (done) {
        request.post({url: channelUrl,
            headers: {"Content-Type": "application/json"},
            body: JSON.stringify({
                name: channelName,
                tags: ['one', 'two'],
            })},
        function (err, response, body) {
            expect(err).toBeNull();
            expect(getProp('statusCode', response)).toBe(201);
            const parse = parseJson(response, __filename);
            const tags = getProp('tags', parse);
            expect(tags).toContain('one');
            expect(tags).toContain('two');
            done();
        });
    });

    it("patches channel " + channelResource, function (done) {
        request.patch({url: channelResource,
            headers: {"Content-Type": "application/json"},
            body: JSON.stringify({ tags: ['one', 'tw*o'] })},
        function (err, response, body) {
            expect(err).toBeNull();
            expect(getProp('statusCode', response)).toBe(400);
            done();
        });
    });
});
