require('./integration_config.js');

var request = require('request');
var http = require('http');
var moment = require('moment');
var channelName = utils.randomChannelName();
var groupName = utils.randomChannelName();
var channelResource = channelUrl + "/" + channelName;
var testName = __filename;

/**
 * This should:
 *
 * 1 - create a channel
 * 2 - post items into the channel
 * 3 - query channel, ensuring that order is descending
 *
 */
describe(testName, function () {

    var start = moment.utc();

    utils.createChannel(channelName);

    for (var i = 0; i < 4; i++) {
        utils.addItem(channelResource);
    }

    var descendingItems = [];

    function callTime(url, items, calls, done) {
        console.log('url ', url);
        request.get({url: channelResource + url, json: true},
            function (err, response, body) {
                expect(err).toBeNull();
                expect(response.statusCode).toBe(200);
                calls++;
                if (items.length === 0) {
                    descendingItems = body._links.uris.reverse();
                    console.log('descendingItems', descendingItems);
                    expect(descendingItems.length).toBe(4);
                    done();
                } else {
                    items = body._links.uris;
                    if (items.length === 4) {
                        console.log('items', items);
                        for (var i = 0; i < items.length; i++) {
                            var item = items[i];
                            var descendingItem = descendingItems[i];
                            expect(item).toBe(descendingItem);
                        }
                        done();
                    } else {
                        done('unable to find 4 items in ' + calls + ' calls');
                    }
                }
            });
    }

    it('gets latest ascending items', function (done) {
        callTime('/latest/4?order=asc&stable=false', [], 0, done);
    });

    it('gets descending earliest', function (done) {
        callTime('/earliest/4?order=desc&stable=false', descendingItems, 0, done);
    });

    it('gets descending latest', function (done) {
        callTime('/latest/4?order=desc&stable=false', descendingItems, 0, done);
    });

    it('gets descending next', function (done) {
        var start = moment.utc().subtract(1, 'minute').format('/YYYY/MM/DD/HH/mm/ss/SSS/');
        callTime(start + 'A/next/4?order=descending&stable=false', descendingItems, 0, done);
    });

    it('gets descending previous', function (done) {
        var start = moment.utc().add(1, 'minute').format('/YYYY/MM/DD/HH/mm/ss/SSS/');
        callTime(start + 'A/previous/4?order=descending&stable=false', descendingItems, 0, done);
    });

    it('gets descending hour', function (done) {
        var start = moment.utc().format('/YYYY/MM/DD/HH');
        callTime(start + '?order=descending&stable=false', descendingItems, 0, done);
    });

    //todo - gfm - bulk gets

});

