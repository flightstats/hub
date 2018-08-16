require('../integration_config');
const request = require('request');
const moment = require('moment');
const { createChannel, getProp } = require('../lib/helpers');
const { getChannelUrl } = require('../lib/config');

const channelUrl = getChannelUrl();
const channelName = utils.randomChannelName();
const channelResource = `${channelUrl}/${channelName}`;
let stableTime = null;
let currentTime = null;
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
        expect(getProp('statusCode', response)).toBe(201);
    });

    it('gets times', function (done) {
        const url = channelResource + '/time';
        request.get({url: url, json: true},
            function (err, response, body) {
                expect(err).toBeNull();
                expect(response.statusCode).toBe(200);
                stableTime = moment(body.stable.millis).utc();
                currentTime = moment(body.now.millis).utc();
                done();
            });
    });

    it('gets unstable next 10 links', function (done) {
        const url = channelResource + currentTime.format('/YYYY/MM/DD/HH/mm/ss') + '/next/10';
        console.log('******', url);
        request.get({url: url, json: true},
            function (err, response, body) {
                expect(err).toBeNull();
                expect(response.statusCode).toBe(200);
                expect(body._links.uris.length).toBe(0);
                expect(body._links.next).toBeUndefined();

                done();
            });
    });

    it('gets stable next 5 links', function (done) {
        const nextTime = stableTime.subtract(5, 'seconds');
        const url = channelResource + nextTime.format('/YYYY/MM/DD/HH/mm/ss') + '/next/10';
        request.get({url: url, json: true},
            function (err, response, body) {
                expect(err).toBeNull();
                expect(response.statusCode).toBe(200);
                expect(body._links.uris.length).toBe(5);
                expect(body._links.next).toBeUndefined();
                done();
            });
    });
});
