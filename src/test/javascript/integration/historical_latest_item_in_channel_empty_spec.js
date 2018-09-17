const moment = require('moment');
const {
    getProp,
    hubClientDelete,
    hubClientGet,
    hubClientPut,
    randomChannelName,
} = require('../lib/helpers');
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

    it("gets latest stable in channel ", async () => {
        const url = `${channelResource}/latest`;
        const response = await hubClientGet(url, headers);
        expect(getProp('statusCode', response)).toEqual(404);
    });

    it("gets latest stable Mutable in channel ", async () => {
        const url = `${channelResource}/latest?epoch=MUTABLE`;
        const response = await hubClientGet(url, headers);
        expect(getProp('statusCode', response)).toEqual(404);
    });

    it("gets latest 10 in default Epoch in channel ", async () => {
        const url = `${channelResource}/latest/10?trace=true`;
        const response = await hubClientGet(url, headers);
        expect(getProp('statusCode', response)).toEqual(404);
    });

    it("gets latest 10 Immutable in channel ", async () => {
        const url = `${channelResource}/latest/10?epoch=IMMUTABLE`;
        const response = await hubClientGet(url, headers);
        expect(getProp('statusCode', response)).toEqual(404);
    });

    it("gets latest 10 Mutable in channel ", async () => {
        const url = `${channelResource}/latest/10?epoch=MUTABLE`;
        const response = await hubClientGet(url, headers);
        expect(getProp('statusCode', response)).toEqual(404);
    });

    afterAll(async () => {
        await hubClientDelete(channelResource);
    });
});
