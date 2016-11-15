require('./integration_config.js');

var request = require('request');
var http = require('http');
var parse = require('parse-link-header');
var channel = utils.randomChannelName();
var moment = require('moment');

var tag = Math.random().toString().replace(".", "");
var testName = __filename;


/**
 * This should:
 * Create a channel with mutableTime
 * add 3 historical items
 * add 3 live items
 *
 * Query items by deirection, verify exclusion
 */
describe(testName, function () {

    var mutableTime = moment.utc().subtract(2, 'years');

    var channelBody = {
        mutableTime: mutableTime.format('YYYY-MM-DDTHH:mm:ss.SSS'),
        tags: [tag, "test"]
    };

    utils.putChannel(channel, false, channelBody, testName);

    var channelURL = hubUrlBase + '/channel/' + channel;

    var items = [];

    function getFormattedUrl(time) {
        return channelURL + time.format('/YYYY/MM/DD/HH/mm/ss/SSS');
    }

    var earliestTime = mutableTime.subtract(2, 'years');

    it('posts historical items to ' + channel, function (done) {
        utils.postItemQ(getFormattedUrl(earliestTime))
            .then(function (value) {
                items.push(value.response.headers.location)
                return utils.postItemQ(getFormattedUrl(earliestTime.add(1, 'years')));
            })
            .then(function (value) {
                items.push(value.response.headers.location)
                return utils.postItemQ(getFormattedUrl(earliestTime.add(6, 'months')));
            })
            .then(function (value) {
                items.push(value.response.headers.location)
                done();
            })
        ;
    });

    it('posts live items to ' + channel, function (done) {
        utils.postItemQ(channelURL)
            .then(function (value) {
                items.push(value.response.headers.location)
                return utils.postItemQ(channelURL);
            })
            .then(function (value) {
                items.push(value.response.headers.location)
                return utils.postItemQ(channelURL);
            })
            .then(function (value) {
                items.push(value.response.headers.location)
                done();
            })
        ;
    });

    /*
     query next N+1 expected
     for each epoch
     */

    function query(queryPath, expected, done) {
        console.log('items', items);
        var url = channelURL + queryPath;
        request.get({url: url, json: true},
            function (err, response, body) {
                expect(err).toBeNull();
                expect(response.statusCode).toBe(200);
                console.log('url ', url);
                var uris = body._links.uris;
                console.log('uris ', uris);
                expect(uris.length).toBe(expected.length);
                for (var i = 0; i < uris.length; i++) {
                    expect(uris[i]).toBe(expected[i]);
                }
                done();
            });
    }

    var next7 = earliestTime.subtract(1, 'month').format('/YYYY/MM/DD/HH/mm/ss/SSS') + "/0/next/7?trace=true&stable=false";


    it('queries next 7 All ' + next7, function (done) {
        query(next7 + '&epoch=ALL', items, done);
    });

    it('queries next 7 Immutable ' + next7, function (done) {
        query(next7 + '&epoch=IMMUTABLE', items.slice(3), done);
    });

    it('queries next 7 Mutable ' + next7, function (done) {
        query(next7 + '&epoch=MUTABLE', items.slice(0, 3), done);
    });


});
