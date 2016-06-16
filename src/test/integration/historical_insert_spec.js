require('./integration_config.js');

var request = require('request');
var http = require('http');
var parse = require('parse-link-header');
var channel = utils.randomChannelName();

var tag = Math.random().toString().replace(".", "");
var testName = __filename;
var channelBody = {
    tags: [tag, "test"],
    ttlDays: 1
};

/**
 * This should:
 * Create a channel
 * Read from a specific time slice in that channel
 * Verify no items
 * Post an item within that time slice
 * Read from the channel at that time slice again
 * Verify our item is now there
 * Verify the payload is the same
 */
describe(testName, function () {

    utils.putChannel(channel, false, channelBody, testName);
    var pointInTime = '2016/06/01/12/00/00/000';
    var pointInTimeURL = hubUrlBase + '/channel/' + channel + '/' + pointInTime;

    it('verifies that channel is empty at: ' + pointInTime, function (done) {
        request.get({
            url: pointInTimeURL
        }, function (error, response, body) {
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

    utils.addItem(pointInTimeURL);

    it('verifies that channel has historical data at: ' + pointInTime, function (done) {
        request.get({
            url: pointInTimeURL
        }, function (error, response, body) {
            expect(err).toBeNull();
            expect(response.statusCode).toBe(200);
            if (response.statusCode == 200) {
                body = utils.parseJson(response, testName);
                uris = body._links.uris;
                expect(uris.length).toBe(1);
                expect(uris[0]).toContain(pointInTime);
            }
            done();
        });
    });

    /*it('gets tag hour ' + tag, function (done) {
        var url = tagUrl + '/time/hour?stable=false&trace=true';
        console.log('calling tag hour ', url)
        request.get({
                url: url,
                headers: {"Content-Type": "application/json"}
            },
            function (err, response, body) {
                expect(err).toBeNull();
                expect(response.statusCode).toBe(200);
                if (response.statusCode == 200) {
                    body = utils.parseJson(response, testName);
                    console.log('parsed tag body', body);
                    if (body._links) {
                        uris = body._links.uris;
                        expect(uris.length).toBe(3);
                        if (uris.length == 3) {
                            expect(uris[0]).toContain(channel);
                            expect(uris[1]).toContain(channelB);
                            expect(uris[2]).toContain(channel);
                        } else {
                            console.log('failing test, not enough uris');
                            expect(false).toBe(true);
                        }
                    }
                } else {
                    console.log('failing test, can\'t get uris . status=' + response.statusCode);
                }
                done();
            });
    }, 2 * 60001);*/




});
