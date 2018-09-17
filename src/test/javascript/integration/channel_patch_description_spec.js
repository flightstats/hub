const { getProp, hubClientDelete, parseJson, randomChannelName } = require('../lib/helpers');
const {
    getChannelUrl,
} = require('../lib/config');

const channelUrl = getChannelUrl();
const request = require('request');
const channelName = randomChannelName();
const channelResource = `${channelUrl}/${channelName}`;

function verifyOptionals (parse) {
    expect(getProp('description', parse)).toBe('describe me');
    expect(getProp('ttlDays', parse)).toBe(9);
}

describe(__filename, function () {
    it("creates channel " + channelName + " at " + channelUrl, function (done) {
        request.post({url: channelUrl,
            headers: {"Content-Type": "application/json"},
            body: JSON.stringify({
                name: channelName,
                description: 'describe me',
                ttlDays: 9,
            })},
        function (err, response, body) {
            expect(err).toBeNull();
            expect(getProp('statusCode', response)).toBe(201);
            const parse = parseJson(response, __filename);
            verifyOptionals(parse);
            done();
        });
    });

    it("verifies channel exists " + channelResource, function (done) {
        request.get({url: channelResource + '?cached=false'},
            function (err, response, body) {
                expect(err).toBeNull();
                expect(getProp('statusCode', response)).toBe(200);
                const parse = parseJson(response, __filename);
                verifyOptionals(parse);
                done();
            });
    });

    it("patches channel with null description " + channelResource, function (done) {
        request.patch({url: channelResource,
            headers: {"Content-Type": "application/json"},
            body: JSON.stringify({ description: 'stuff' })},
        function (err, response, body) {
            expect(err).toBeNull();
            expect(getProp('statusCode', response)).toBe(200);
            const parse = parseJson(response, __filename);
            expect(getProp('description', parse)).toBe("stuff");
            done();
        });
    });

    it("verifies channel exists with correct description " + channelResource, function (done) {
        request.get({url: channelResource + '?cached=false'},
            function (err, response, body) {
                expect(err).toBeNull();
                expect(getProp('statusCode', response)).toBe(200);
                const parse = parseJson(response, __filename);
                expect(getProp('description', parse)).toBe("stuff");
                done();
            });
    });

    afterAll(async () => {
        await hubClientDelete(channelResource);
    });
});
