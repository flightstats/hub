require('../integration_config');
const { getProp } = require('../lib/helpers');
const {
    getChannelUrl,
} = require('../lib/config');

const channelUrl = getChannelUrl();
const request = require('request');
const channelName = utils.randomChannelName();
const channelResource = `${channelUrl}/${channelName}`;

function verifyOptionals (parse) {
    expect(getProp('description', parse)).toBe('describe me');
    const tags = getProp('tags', parse);
    expect(tags).toContain('one');
    expect(tags).toContain('two');
}

describe(__filename, function () {
    it(`creates channel ${channelName} at ${channelUrl}`, function (done) {
        request.post({url: channelUrl,
            headers: {"Content-Type": "application/json"},
            body: JSON.stringify({
                name: channelName,
                description: 'describe me',
                ttlDays: 10,
                contentSizeKB: 3,
                peakRequestRateSeconds: 2,
                tags: ['one', 'two'],
            })},
        function (err, response, body) {
            expect(err).toBeNull();
            expect(getProp('statusCode', response)).toBe(201);
            const parse = utils.parseJson(response, __filename);
            expect(getProp('ttlDays', parse)).toBe(10);
            done();
        });
    });

    it(`verifies channel exists ${channelResource}`, function (done) {
        request.get({url: channelResource + '?cached=false'},
            function (err, response, body) {
                expect(err).toBeNull();
                expect(getProp('statusCode', response)).toBe(200);
                const parse = utils.parseJson(response, __filename);
                expect(getProp('ttlDays', parse)).toBe(10);
                verifyOptionals(parse);
                done();
            });
    });

    it(`patches channel ${channelResource}`, function (done) {
        request.patch({url: channelResource,
            headers: {"Content-Type": "application/json"},
            body: JSON.stringify({ ttlDays: 12 })},
        function (err, response, body) {
            expect(err).toBeNull();
            expect(getProp('statusCode', response)).toBe(200);
            const parse = utils.parseJson(response, __filename);
            expect(getProp('ttlDays', parse)).toBe(12);
            verifyOptionals(parse);
            done();
        });
    });

    it(`verifies channel exists with correct ttl ${channelResource}`, function (done) {
        request.get({url: channelResource + '?cached=false'},
            function (err, response, body) {
                expect(err).toBeNull();
                expect(getProp('statusCode', response)).toBe(200);
                const parse = utils.parseJson(response, __filename);
                expect(getProp('ttlDays', parse)).toBe(12);
                verifyOptionals(parse);
                done();
            });
    });
});
