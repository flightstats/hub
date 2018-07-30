require('../integration_config');
const request = require('request');
const moment = require('moment');
const {
    fromObjectPath,
    getProp,
    hubClientPut,
} = require('../lib/helpers');

const channel = utils.randomChannelName();
const tag = Math.random().toString().replace(".", "");
const mutableTime = moment.utc().subtract(1, 'minute');
const channelResource = `${channelUrl}/${channel}`;
const headers = { 'Content-Type': 'application/json' };
const pointInThePastURL = channelResource + '/' + mutableTime.format('YYYY/MM/DD/HH/mm/ss/SSS');
const channelBody = {
    ttlDays: 0,
    mutableTime: mutableTime.format('YYYY-MM-DDTHH:mm:ss.SSS'),
    tags: [tag, "test"],
};
let historicalLocation = null;
let liveLocation = null;
let liveTime = null;
/**
 * This should:
 * Create a channel
 * change the channel to have a mutableTime
 * insert an item before the mutableTime, verify item with get
 * insert an item into now, verify item with get
 * Query items by time, verify exclusion
 */
describe(__filename, function () {
    const timeQuery = (query, expected, done) => {
        const url = `${channelResource}${query}`;
        request.get({url: url, json: true},
            function (err, response, body) {
                expect(err).toBeNull();
                expect(getProp('statusCode', response)).toBe(200);
                const uris = fromObjectPath(['_links', 'uris'], body) || [];
                expect(uris.length).toBe(expected.length);
                for (let i = 0; i < uris.length; i++) {
                    expect(expected[i]).toBeDefined();
                    expect(uris[i]).toBe(expected[i]);
                }
                done();
            });
    };

    const queryTimes = (format, done) => {
        const liveQuery = liveTime.format(format);
        const mutableQuery = mutableTime.format(format);

        let queryAll = done;
        if (liveQuery === mutableQuery) {
            queryAll = function () {
                timeQuery(mutableQuery + '?epoch=ALL&trace=true&stable=false', [historicalLocation, liveLocation], done);
            };
        }

        const queryMutable = function () {
            timeQuery(mutableQuery + '?epoch=MUTABLE&trace=true&stable=false', [historicalLocation], queryAll);
        };
        timeQuery(liveQuery + '?epoch=IMMUTABLE&trace=true&stable=false', [liveLocation], queryMutable);
    };

    beforeAll(async () => {
        const response = await hubClientPut(channelResource, headers, { ttlDays: 20 });
        expect(getProp('statusCode', response)).toEqual(201);
    });

    utils.itRefreshesChannels();

    it('updates the channel to mutableTime', async () => {
        const response = await hubClientPut(channelResource, headers, channelBody);
        expect(getProp('statusCode', response)).toEqual(201);
    });

    utils.itRefreshesChannels();

    it(`posts historical item to ${channel}`, function (done) {
        utils.postItemQ(pointInThePastURL)
            .then(function (value) {
                historicalLocation = fromObjectPath(['response', 'headers', 'location'], value);
                done();
            });
    });

    it(`gets historical item from ${historicalLocation}`, function (done) {
        request.get({url: historicalLocation},
            function (err, response, body) {
                expect(err).toBeNull();
                expect(getProp('statusCode', response)).toBe(200);
                done();
            });
    });

    it(`posts live item to ${channel}`, function (done) {
        utils.postItemQ(channelResource)
            .then(function (value) {
                liveLocation = fromObjectPath(['response', 'headers', 'location'], value);
                liveTime = moment((liveLocation || '').substring(channelResource.length), '/YYYY/MM/DD/HH/mm/ss/SSS');
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

    it('mutable item by day', function (done) {
        queryTimes('/YYYY/MM/DD', done);
    });

    it('mutable item by hour', function (done) {
        queryTimes('/YYYY/MM/DD/HH', done);
    });

    it('mutable item by minute', function (done) {
        queryTimes('/YYYY/MM/DD/HH/mm', done);
    });

    it('mutable item by second', function (done) {
        queryTimes('/YYYY/MM/DD/HH/mm/ss', done);
    });

    it('mutable item by millis', function (done) {
        queryTimes('/YYYY/MM/DD/HH/mm/ss/SSS', done);
    });
});
