require('../integration_config');
const moment = require('moment');
const { getProp, hubClientPut } = require('../lib/helpers');

const channel = utils.randomChannelName();
const channelResource = `${channelUrl}/${channel}`;
const headers = { 'Content-Type': 'application/json' };
const mutableTime = moment.utc().subtract(1, 'minute');
const channelBody = {
    mutableTime: mutableTime.format('YYYY-MM-DDTHH:mm:ss.SSS'),
    tags: ["test"],
};
let channelCreated = false;
/**
 * create a channel
 * get earliest returns 404
 * get earliest/10 returns 404
 */
describe(__filename, function () {
    beforeAll(async () => {
        const response = await hubClientPut(channelResource, headers, channelBody);
        if (getProp('statusCode', response) === 201) {
            channelCreated = true;
        }
    });

    it("gets earliest in default Epoch in channel ", function (done) {
        if (!channelCreated) return done.fail('channel not created in before block');
        utils.getLocation(channelResource + '/earliest?trace=true', 404, false, done);
    });

    it("gets earliest Immutable in channel ", function (done) {
        if (!channelCreated) return done.fail('channel not created in before block');
        utils.getLocation(channelResource + '/earliest?epoch=IMMUTABLE', 404, false, done);
    });

    it("gets earliest Mutable in channel ", function (done) {
        if (!channelCreated) return done.fail('channel not created in before block');
        utils.getLocation(channelResource + '/earliest?epoch=MUTABLE', 404, false, done);
    });

    it("gets earliest 10 in default Epoch in channel ", function (done) {
        if (!channelCreated) return done.fail('channel not created in before block');
        utils.getLocation(channelResource + '/earliest/10?trace=true', 404, false, done);
    });

    it("gets earliest 10 Immutable in channel ", function (done) {
        if (!channelCreated) return done.fail('channel not created in before block');
        utils.getLocation(channelResource + '/earliest/10?epoch=IMMUTABLE', 404, false, done);
    });

    it("gets earliest 10 Mutable in channel ", function (done) {
        if (!channelCreated) return done.fail('channel not created in before block');
        utils.getLocation(channelResource + '/earliest/10?epoch=MUTABLE', 404, false, done);
    });
});
