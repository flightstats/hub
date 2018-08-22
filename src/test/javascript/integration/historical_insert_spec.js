require('../integration_config');
const request = require('request');
const moment = require('moment');
const {
    fromObjectPath,
    getProp,
    hubClientPut,
    hubClientPostTestItem,
} = require('../lib/helpers');
const {
    getChannelUrl,
} = require('../lib/config');

const channelUrl = getChannelUrl();
const mutableTime = moment.utc().subtract(1, 'minute');
const channelBody = {
    mutableTime: mutableTime.format('YYYY-MM-DDTHH:mm:ss.SSS'),
    tags: ["test"],
};
const channel = utils.randomChannelName();
const channelResource = `${channelUrl}/${channel}`;
const headers = { 'Content-Type': 'application/json' };
const pointInThePastURL = channelResource + '/' + mutableTime.format('YYYY/MM/DD/HH/mm/ss/SSS');
let historicalLocation = null;
let liveLocation = null;
let hashItem = null;
/**
 * This should:
 * Create a channel with mutableTime
 * insert an item before the mutableTime, verify item with get
 * insert an item into now, verify item with get
 * insert an item with a user defined hash
 */
describe(__filename, function () {
    beforeAll(async () => {
        const response = await hubClientPut(channelResource, headers, channelBody);
        expect(getProp('statusCode', response)).toEqual(201);
    });

    it(`posts historical item to ${channel}`, async () => {
        const response = await hubClientPostTestItem(pointInThePastURL);
        historicalLocation = fromObjectPath(['headers', 'location'], response);
    });

    it(`gets historical item from ${historicalLocation}`, function (done) {
        request.get({url: historicalLocation},
            function (err, response, body) {
                expect(err).toBeNull();
                expect(getProp('statusCode', response)).toBe(200);
                done();
            });
    });

    it(`posts live item to ${channel}`, async () => {
        const response = await hubClientPostTestItem(channelResource);
        liveLocation = fromObjectPath(['headers', 'location'], response);
    });

    it(`gets live item from ${liveLocation}`, function (done) {
        request.get({url: liveLocation},
            function (err, response, body) {
                expect(err).toBeNull();
                expect(getProp('statusCode', response)).toBe(200);
                done();
            });
    });

    it(`posts historical item to ${channel}`, async () => {
        const response = await hubClientPostTestItem(`${pointInThePastURL}/abcdefg`);
        hashItem = fromObjectPath(['headers', 'location'], response);
        expect(hashItem).toContain('/abcdefg');
    });

    it(`gets historical item from ${hashItem}`, function (done) {
        request.get({url: hashItem},
            function (err, response, body) {
                expect(err).toBeNull();
                expect(getProp('statusCode', response)).toBe(200);
                done();
            });
    });
});
