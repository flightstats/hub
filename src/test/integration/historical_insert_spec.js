require('./integration_config.js');

var request = require('request');
var http = require('http');
var parse = require('parse-link-header');
var channel = utils.randomChannelName();
var moment = require('moment');

var tag = Math.random().toString().replace(".", "");
var testName = __filename;
var channelBody = {
    historical: true,
    tags: [tag, "test"],
    ttlDays: 365
};

/**
 * This should:
 * Create a channel
 * Verify no items are found in the past
 * Insert an item into the past
 * Verify our inserted item is found and the payload is the same
 * Verify an error is thrown when inserting an item near 'now'
 */
describe(testName, function () {

    utils.putChannel(channel, false, channelBody, testName);
    var channelURL = hubUrlBase + '/channel/' + channel;
    var pointInThePast = '2016/06/01/12/00/00/000';
    var pointInThePastURL = channelURL + '/' + pointInThePast;
    var pointCloseToNow = moment.utc().format('YYYY/MM/DD/HH/mm/ss/SSS');
    var pointCloseToNowURL = channelURL + '/' + pointCloseToNow;
    var spokeTTLMinutes = 60;

    it('verifies that channel is empty at: ' + pointInThePast, function (done) {
        request.get({
            url: pointInThePastURL
        }, function (err, response, body) {
            expect(err).toBeNull();
            expect(response.statusCode).toBe(200);
            if (response.statusCode == 200) {
                body = utils.parseJson(response, testName);
                uris = body._links.uris;
                expect(uris.length).toBe(0);
            }
            done();
        });
    });

    utils.addItem(pointInThePastURL);

    it('verifies that channel has historical data at: ' + pointInThePast, function (done) {
        request.get({
            url: pointInThePastURL
        }, function (err, response, body) {
            expect(err).toBeNull();
            expect(response.statusCode).toBe(200);
            if (response.statusCode == 200) {
                body = utils.parseJson(response, testName);
                console.log('body', body);
                uris = body._links.uris;
                expect(uris.length).toBe(1);
                expect(uris[0]).toContain(pointInThePast);
            }
            done();
        });
    });

    it('verifies that historical inserts fail if within ' + spokeTTLMinutes + 'minutes of now.', function (done) {
        request.post({
            url: pointCloseToNowURL
        }, function (err, response) {
            expect(err).toBeNull();
            expect(response.statusCode).toBe(400);
            done();
        })

    });

    utils.putChannel(channel, false, {historical: false}, testName, 400);

});
