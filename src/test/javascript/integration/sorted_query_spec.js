require('../integration_config');

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

    var next = moment.utc().subtract(1, 'minute').format('/YYYY/MM/DD/HH/mm/ss/SSS/')
        + 'A/next/4?order=descending&stable=false';
    it('gets descending next', function (done) {
        callTime(next, descendingItems, 0, done);
    });

    it('gets descending previous', function (done) {
        var start = moment.utc().add(1, 'minute').format('/YYYY/MM/DD/HH/mm/ss/SSS/');
        callTime(start + 'A/previous/4?order=descending&stable=false', descendingItems, 0, done);
    });

    it('gets descending hour', function (done) {
        var start = moment.utc().format('/YYYY/MM/DD/HH');
        callTime(start + '?order=descending&stable=false', descendingItems, 0, done);
    });

    function checkIndex(body, start, index) {
        var indexOf = body.indexOf(descendingItems[index], start);
        console.log('indexOf', indexOf);
        expect(indexOf).toBeGreaterThan(0);
        return indexOf;
    }

    var MINUTE = 60 * 1000;
    var execute = true;

    it("checks the hub for large item suport", function (done) {
        request.get({
                url: hubUrlBase + '/internal/properties'
            },
            function (err, response, body) {
                expect(err).toBeNull();
                expect(response.statusCode).toBe(200);
                var parse = utils.parseJson(response, testName);
                console.log(response.body);
                var hubType = parse['properties']['hub.type'];
                execute = hubType === 'aws';
                console.log(hubType, 'execute', execute);
                done();
            });
    }, 5 * MINUTE);


    it('bulk get', function (done) {
        if (execute) {
            var url = next + '&bulk=true';
            console.log('url ', url);
            request.get({url: channelResource + url, json: true},
                function (err, response, body) {
                    expect(err).toBeNull();
                    expect(response.statusCode).toBe(200);
                    console.log('bulk body', body);
                    var index = checkIndex(body, 0, 0);
                    index = checkIndex(body, index, 1);
                    index = checkIndex(body, index, 2);
                    checkIndex(body, index, 3);
                    done();
                });
        } else {
            done();
        }
    })



});

