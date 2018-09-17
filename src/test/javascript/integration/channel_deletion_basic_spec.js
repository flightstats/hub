const request = require('request');
const { createChannel, getProp, randomChannelName } = require('../lib/helpers');
const {
    getChannelUrl,
} = require('../lib/config');

const channelUrl = getChannelUrl();
const channelName = randomChannelName();
const channelResource = `${channelUrl}/${channelName}`;
let channelCreated = false;

describe(__filename, function () {
    beforeAll(async () => {
        const result = await createChannel(channelName);
        if (getProp('statusCode', result) === 201) {
            console.log(`created channel for ${channelName}`);
            channelCreated = true;
        }
    });

    it("deletes channel " + channelName, function (done) {
        if (!channelCreated) return done.fail('channel not created in before block');
        request.del({url: channelResource},
            function (err, response, body) {
                console.log('body', body);
                expect(err).toBeNull();
                expect(getProp('statusCode', response)).toBe(202);
                done();
            });
    }, 65000);

    it("gets deleted channel " + channelName, function (done) {
        if (!channelCreated) return done.fail('channel not created in before block');
        request.get({url: channelResource + '?cached=false'},
            function (err, response, body) {
                expect(err).toBeNull();
                expect(getProp('statusCode', response)).toBe(404);
                done();
            });
    });
});
