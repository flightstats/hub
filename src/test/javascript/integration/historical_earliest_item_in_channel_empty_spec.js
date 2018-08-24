const moment = require('moment');
const { getProp, hubClientGet, hubClientPut, randomChannelName } = require('../lib/helpers');
const {
    getChannelUrl,
} = require('../lib/config');

const channelUrl = getChannelUrl();
const channel = randomChannelName();
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

    it("gets earliest in default Epoch in channel ", async () => {
        if (!channelCreated) return fail('channel not created in before block');
        const url = `${channelResource}/earliest?trace=true`;
        const response = await hubClientGet(url, headers);
        expect(getProp('statusCode', response)).toEqual(404);
    });

    it("gets earliest Immutable in channel ", async () => {
        if (!channelCreated) return fail('channel not created in before block');
        const url = `${channelResource}/earliest?epoch=IMMUTABLE`;
        const response = await hubClientGet(url, headers);
        expect(getProp('statusCode', response)).toEqual(404);
    });

    it("gets earliest Mutable in channel ", async () => {
        if (!channelCreated) return fail('channel not created in before block');
        const url = `${channelResource}/earliest?epoch=MUTABLE`;
        const response = await hubClientGet(url, headers);
        expect(getProp('statusCode', response)).toEqual(404);
    });

    it("gets earliest 10 in default Epoch in channel ", async () => {
        if (!channelCreated) return fail('channel not created in before block');
        const url = `${channelResource}/earliest/10?trace=true`;
        const response = await hubClientGet(url, headers);
        expect(getProp('statusCode', response)).toEqual(404);
    });

    it("gets earliest 10 Immutable in channel ", async () => {
        if (!channelCreated) return fail('channel not created in before block');
        const url = `${channelResource}/earliest/10?epoch=IMMUTABLE`;
        const response = await hubClientGet(url, headers);
        expect(getProp('statusCode', response)).toEqual(404);
    });

    it("gets earliest 10 Mutable in channel ", async () => {
        if (!channelCreated) return fail('channel not created in before block');
        const url = `${channelResource}/earliest/10?epoch=MUTABLE`;
        const response = await hubClientGet(url, headers);
        expect(getProp('statusCode', response)).toEqual(404);
    });
});
