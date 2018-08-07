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

/**
 * create a channel
 * get latest returns 404
 * get latest/10 returns 404
 */
describe(__filename, function () {
    beforeAll(async () => {
        const response = await hubClientPut(channelResource, headers, channelBody);
        expect(getProp('statusCode', response)).toEqual(201);
    });

    it("gets latest stable in channel ", function (done) {
        utils.getLocation(`${channelResource}/latest`, 404, false, done);
    });

    it("gets latest stable Mutable in channel ", function (done) {
        utils.getLocation(`${channelResource}/latest?epoch=MUTABLE`, 404, false, done);
    });

    it("gets latest 10 in default Epoch in channel ", function (done) {
        utils.getLocation(`${channelResource}/latest/10?trace=true`, 404, false, done);
    });

    it("gets latest 10 Immutable in channel ", function (done) {
        utils.getLocation(`${channelResource}/latest/10?epoch=IMMUTABLE`, 404, false, done);
    });

    it("gets latest 10 Mutable in channel ", function (done) {
        utils.getLocation(`${channelResource}/latest/10?epoch=MUTABLE`, 404, false, done);
    });
});
