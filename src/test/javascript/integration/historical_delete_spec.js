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
const channel = utils.randomChannelName();
const mutableTime = moment.utc().subtract(1, 'minute');
const channelBody = {
    mutableTime: mutableTime.format('YYYY-MM-DDTHH:mm:ss.SSS'),
    tags: ["test"],
};
const headers = { 'Content-Type': 'application/json' };
const url = `${channelUrl}/${channel}`;
const pointInThePastURL = `${url}/${mutableTime.format('YYYY/MM/DD/HH/mm/ss/SSS')}`;
let historicalLocation;
let liveLocation;
/**
 * This should:
 * Create a channel with mutableTime
 * insert an item before the mutableTime
 * delete the item
 * confirm 404 with get
 *
 * insert a live item
 * live item deletion fails
 * live item still exists
 *
 */
describe(__filename, function () {
    beforeAll(async () => {
        const response = await hubClientPut(url, headers, channelBody);
        expect(getProp('statusCode', response)).toEqual(201);
        const response1 = await hubClientPostTestItem(pointInThePastURL);
        historicalLocation = fromObjectPath(['headers', 'location'], response1);
    });

    it('deletes historical item ', function (done) {
        console.log('historicalLocation', historicalLocation);
        request.del({url: `${historicalLocation}?trace=true`},
            function (err, response, body) {
                expect(err).toBeNull();
                expect(getProp('statusCode', response)).toBe(204);
                done();
            });
    });

    it('gets 404 for deleted historical item ', function (done) {
        request.get({url: historicalLocation},
            function (err, response, body) {
                expect(err).toBeNull();
                expect(getProp('statusCode', response)).toBe(404);
                done();
            });
    });

    it(`posts live item to ${channel}`, async () => {
        const response = await hubClientPostTestItem(url);
        liveLocation = fromObjectPath(['headers', 'location'], response);
    });

    it('deletes live item ', function (done) {
        console.log('liveLocation', liveLocation);
        request.del({url: `${liveLocation}?trace=true`},
            function (err, response, body) {
                expect(err).toBeNull();
                expect(getProp('statusCode', response)).toBe(405);
                done();
            });
    });

    it(`gets live item from ${liveLocation}`, function (done) {
        request.get({url: liveLocation},
            function (err, response, body) {
                expect(err).toBeNull();
                expect(getProp('statusCode', response)).toBe(200);
                done();
            });
    });
});
