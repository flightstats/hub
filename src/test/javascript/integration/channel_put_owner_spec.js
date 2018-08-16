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
    expect(getProp('owner', parse)).toBe('the man');
}

describe(__filename, function () {
    console.log('channel url', channelResource);
    it(`creates channel ${channelName} at ${channelUrl}`, function (done) {
        request.put({
            url: channelResource,
            headers: {"Content-Type": "application/json"},
            body: JSON.stringify({
                owner: 'the man',
            }),
        },
        function (err, response, body) {
            expect(err).toBeNull();
            expect(getProp('statusCode', response)).toBe(201);
            const parse = utils.parseJson(response, __filename);
            verifyOptionals(parse);
            done();
        });
    });

    it(`verifies channel exists ${channelResource}`, function (done) {
        request.get({url: channelResource + '?cached=false'},
            function (err, response, body) {
                expect(err).toBeNull();
                expect(getProp('statusCode', response)).toBe(200);
                const parse = utils.parseJson(response, __filename);
                verifyOptionals(parse);
                done();
            });
    });

    it(`puts channel with null owner ${channelResource}`, function (done) {
        request.put({
            url: channelResource,
            headers: {"Content-Type": "application/json"},
            body: JSON.stringify({owner: 'stuff'}),
        },
        function (err, response, body) {
            expect(err).toBeNull();
            expect(getProp('statusCode', response)).toBe(201);
            const parse = utils.parseJson(response, __filename);
            expect(getProp('owner', parse)).toBe("stuff");
            done();
        });
    });

    it(`verifies channel exists with correct owner ${channelResource}`, function (done) {
        request.get({url: channelResource + '?cached=false'},
            function (err, response, body) {
                expect(err).toBeNull();
                expect(getProp('statusCode', response)).toBe(200);
                const parse = utils.parseJson(response, __filename);
                expect(getProp('owner', parse)).toBe("stuff");
                done();
            });
    });
});
