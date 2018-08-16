require('../integration_config');
const {
    fromObjectPath,
    getProp,
    hubClientPut,
} = require('../lib/helpers');
const {
    getChannelUrl,
} = require('../lib/config');

const channelUrl = getChannelUrl();
const request = require('request');
const channelName = utils.randomChannelName();
const channelResource = `${channelUrl}/${channelName}`;
const headers = { 'Content-Type': 'application/json' };
const testName = __filename;
const moment = require('moment');
const mutableTime = moment.utc().subtract(1, 'minute');
const SIZE = 41 * 1024 * 1024;
const pointInThePastURL = channelResource + '/' + mutableTime.format('YYYY/MM/DD/HH/mm/ss/SSS');
let hashItem = null;
const channelBody = {
    mutableTime: mutableTime.format('YYYY-MM-DDTHH:mm:ss.SSS'),
    tags: ["test"],
};
const MINUTE = 60 * 1000;

/**
 * 1 - create a large payload channel
 * 2 - post a large item historical (100+ MB)
 * 3 - fetch the item and verify bytes
 */

describe(testName, function () {
    beforeAll(async () => {
        const response = await hubClientPut(channelResource, headers, channelBody);
        expect(getProp('statusCode', response)).toEqual(201);
    });

    it("posts a large historical item to " + channelName, function (done) {
        request.post({
            url: pointInThePastURL + '/large01',
            headers: {'Content-Type': "text/plain"},
            body: Array(SIZE).join("a"),
        },
        function (err, response, body) {
            hashItem = fromObjectPath(['headers', 'location'], response);
            expect(err).toBeNull();
            done();
        });
    }, 5 * MINUTE);

    it("gets item " + channelName, function (done) {
        request.get({url: hashItem},
            function (err, response, body) {
                expect(err).toBeNull();
                expect(getProp('statusCode', response)).toBe(200);
                done();
            });
    }, 5 * MINUTE);
});
