require('../integration_config');
const moment = require('moment');
const { getHubUrlBase } = require('../lib/config');
const {
    fromObjectPath,
    getProp,
    hubClientGet,
} = require('../lib/helpers');
const hubUrl = getHubUrlBase();
console.log(hubUrl);
const channelUrl = `${hubUrl}/channel/load_test_1`;
const twoDaysAgo = `${channelUrl}/${moment().utc().subtract(2, 'days').format('YYYY/MM/DD/HH/mm')}`;
const headers = { 'Content-Type': 'application/json' };
// TODO: I don't think is being used ????
let uris = [];
let channel = null;
let previous = '';

const getLocation = async (url) => {
    console.log('get location ', url);
    const res = await hubClientGet(url, headers);
    expect(getProp('statusCode', res)).toBe(303);
    const location = fromObjectPath(['headers', 'location'], res);
    console.log('res.headers.location', location);
    return location;
};

const getAndCompare = async (url) => {
    console.log('gets ', url);
    const res = await hubClientGet(url);
    expect(getProp('statusCode', res)).toBe(200);
    const links = fromObjectPath(['body', '_links', 'uris'], res) || [];
    return links.every((link, arr, index) =>
        uris.includes(link) && uris.indexOf(link) === index);
};
/**
 * This assumes that a channel is being written to continuously.
 *
 * This should :
 * 1 - Get an hour's worth of data from two days ago.
 * 2 - Call previous on the first item
 * 3 - From the previous item, call next/N, and verify same set
 * Call next on the last item
 * From the next item, call previous/N , and verify same set
 *
 */
// jasmine-node --forceexit --captureExceptions --config hubUrl hub-v2.svc.dev verify_pagination_spec.js
describe(__filename, function () {
    it('0 - loads channel info ', async () => {
        console.log('channelUrl', channelUrl);
        const res = await hubClientGet(channelUrl, headers);
        expect(getProp('statusCode', res)).toBe(200);
        channel = getProp('body', res) || {};
        expect(channel.ttlDays).toBeGreaterThan(0);
        const timeLink = fromObjectPath(['_links', 'time', 'href'], res);
        const res2 = await hubClientGet(timeLink);
        expect(getProp('statusCode', res2)).toBe(200);
        const millis = fromObjectPath(['body', 'now', 'millis'], res2);
        channel.millis = millis;
        expect(millis).toBeGreaterThan(1435865512097);
        console.log('channel', channel);
    }, 60 * 1000);

    it('1 - gets two days ago', async () => {
        console.log('twoDaysAgo', twoDaysAgo);
        const res = await hubClientGet(twoDaysAgo);
        expect(getProp('statusCode', res)).toBe(200);
        uris = fromObjectPath(['body', '_links', 'uris'], res);
        console.log('length', uris.length);
    }, 60 * 1000);

    it(`2 - gets previous ${uris[0]}`, async () => {
        previous = await getLocation(`${uris[0]}/previous`);
    }, 60 * 1001);

    it(`3 - gets next N from ${previous}`, async () => {
        await getAndCompare(`${previous}/next/${uris.length}`);
    }, 60 * 1002);

    var next = '';

    it(`4 - gets next ${uris[uris.length - 1]}`, async () => {
        next = await getLocation(`${uris[uris.length - 1]}/next`);
    }, 60 * 1003);

    it(`5 - gets previous N from ${next}`, async () => {
        await getAndCompare(`${next}/previous/${uris.length}`);
    }, 60 * 1004);

    it('6 - gets earliest', async () => {
        const earlyLink = fromObjectPath(['_links', 'earliest', 'href'], channel);
        console.log('earliest', earlyLink);
        const res = await hubClientGet(earlyLink, headers);
        expect(getProp('statusCode', res)).toBe(303);
        const time = moment(channel.millis).subtract(channel.ttlDays, 'days').utc();
        const location = fromObjectPath(['headers', 'location'], res) || '';
        let timeUrlSegments = location.substring(channelUrl.length);
        timeUrlSegments = timeUrlSegments.substring(0, timeUrlSegments.lastIndexOf('/'));
        const earliestTime = moment(`${timeUrlSegments}+0000`, '/YYYY/MM/DD/HH/mm/ss/SSS Z');
        expect(earliestTime.isAfter(time)).toBe(true);
        expect(earliestTime.isBefore(time.add(1, 'hours'))).toBe(true);
    }, 60 * 1000);
});
