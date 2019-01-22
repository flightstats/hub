const request = require('request');
const moment = require('moment');
const {
    createChannel,
    fromObjectPath,
    getProp,
    hubClientDelete,
    hubClientGet,
    itSleeps,
    randomChannelName,
} = require('../lib/helpers');
const { getChannelUrl } = require('../lib/config');

const channelUrl = getChannelUrl();
const channelName = randomChannelName();
const channelResource = `${channelUrl}/${channelName}`;
const testContext = {
    stableTime: null,
    currentTime: null,
};
const headers = {
    'Content-Type': 'application/json',
};
/**
 * This should:
 *
 * 1 - get the current time
 * 2 - call next/N on the current time, should get less than a full compliment
 * 3 - subtract X seconds, call next/N
 */

describe(__filename, function () {
    beforeAll(async () => {
        const response = await createChannel(channelName);
        expect(getProp('statusCode', response)).toEqual(201);
    });

    it('gets times', async () => {
        const response = await hubClientGet(`${channelResource}/time`, headers);
        expect(getProp('statusCode', response)).toEqual(200);
        const stableTimeMillis = fromObjectPath(['body', 'stable', 'millis'], response);
        const currentTimeMillis = fromObjectPath(['body', 'stable', 'millis'], response);
        testContext.stableTime = moment(stableTimeMillis).utc();
        testContext.currentTime = moment(currentTimeMillis).utc();
    });

    it('gets stable next 5 links', async () => {
        await itSleeps(1000);
        const nextTime = testContext.stableTime.subtract(5, 'seconds');
        const url = `${channelResource}${nextTime.format('/YYYY/MM/DD/HH/mm/ss')}/next/10`;
        const response = await hubClientGet(url, headers);
        expect(getProp('statusCode', response)).toBe(200);
        const uris = fromObjectPath(['body', '_links', 'uris'], response);
        expect(uris.length).toBeLessThan(10);
        expect(uris.length).toBeGreaterThan(0);
    });

    afterAll(async () => {
        await hubClientDelete(channelResource);
    });
});
