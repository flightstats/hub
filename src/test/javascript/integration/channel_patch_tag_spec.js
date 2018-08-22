require('../integration_config');
const { getProp, parseJson } = require('../lib/helpers');
const {
    getChannelUrl,
} = require('../lib/config');

const channelUrl = getChannelUrl();
const request = require('request');
const channelName = utils.randomChannelName();
const channelResource = `${channelUrl}/${channelName}`;

function verifyOptionals (parse) {
    expect(getProp('description', parse)).toBe('describe me');
    expect(getProp('ttlDays', parse)).toBe(9);
}

describe(__filename, function () {
    it(`creates channel ${channelName} at ${channelUrl}`, function (done) {
        request.post({url: channelUrl,
            headers: {"Content-Type": "application/json"},
            body: JSON.stringify({
                name: channelName,
                description: 'describe me',
                ttlDays: 9,
                contentSizeKB: 3,
                peakRequestRateSeconds: 2,
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

    it(`verifies channel exists ${channelResource}`, function (done) {
        request.get({url: `${channelResource}?cached=false`},
            function (err, response, body) {
                expect(err).toBeNull();
                expect(getProp('statusCode', response)).toBe(200);
                const parse = parseJson(response, __filename);
                const tags = getProp('tags', parse);
                expect(tags).toContain('one');
                expect(tags).toContain('two');
                verifyOptionals(parse);
                done();
            });
    });

    function verifyPatched (parse) {
        const tags = getProp('tags', parse);
        if (tags) {
            expect(tags).toContain('one');
            expect(tags).toContain('three');
            expect(tags).not.toContain('two');
        }
        verifyOptionals(parse);
    }

    it(`patches channel ${channelResource}`, function (done) {
        request.patch({url: channelResource,
            headers: {"Content-Type": "application/json"},
            body: JSON.stringify({ tags: ['one', 'three'] })},
        function (err, response, body) {
            expect(err).toBeNull();
            expect(getProp('statusCode', response)).toBe(200);
            const parse = parseJson(response, __filename);
            verifyPatched(parse);
            done();
        });
    });

    it(`verifies channel exists with correct tags ${channelResource}`, function (done) {
        request.get({url: channelResource + '?cached=false'},
            function (err, response, body) {
                expect(err).toBeNull();
                expect(getProp('statusCode', response)).toBe(200);
                const parse = parseJson(response, __filename);
                verifyPatched(parse);
                done();
            });
    });
});
