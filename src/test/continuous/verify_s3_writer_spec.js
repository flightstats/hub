var agent = require('superagent');
var async = require('async');
var moment = require('moment');
var _ = require('lodash');
var testName = __filename;
var hubUrl = process.env.hubUrl;
hubUrl = 'http://' + hubUrl + '/channel';
console.log(hubUrl);

var timeout = 5 * 60 * 1000;
var minute_format = '/YYYY/MM/DD/HH/mm';
var startOffset = parseInt(process.env.startOffset) || 19;
var endOffset = parseInt(process.env.endOffset) || 30;
var testPercent = parseInt(process.env.testPercent) || 50;

/**
 * This should load all the channels in the hub.
 */
describe(testName, function () {

    var channels = [];
    var channelTimes = [];

    it('loads channels', function (done) {
        agent
            .get(hubUrl)
            .set('Accept', 'application/json')
            .end(function (res) {
                expect(res.error).toBe(false);
                var allChannels = res.body._links.channels;
                allChannels.forEach(function (channel) {
                    console.log('channel', channel);
                    if (channel.name.substring(0, 4) !== 'test') {
                        channels.push(channel);
                    }
                })
                done();
            })
    }, timeout);

    it('loads channel data', function (done) {
        async.eachLimit(channels, 10,
            function (channel, callback) {
                console.log('calling', channel);
                agent
                    .get(channel.href)
                    .set('Accept', 'application/json')
                    .end(function (res) {
                        expect(res.error).toBe(false);
                        channel.storage = res.body.storage;
                        channel.start = moment.utc();
                        if (res.body.historical) {
                            channel.history = true;
                        }
                        callback();
                    })
            }, function (err) {
                done(err);
            });

    }, timeout);

    it('loads historical channel data', function (done) {
        async.eachLimit(channels, 10,
            function (channel, callback) {
                if (!channel.history) {
                    callback();
                    return
                }
                console.log('history check ', channel);
                agent
                    .get(channel.href + '/latest')
                    .redirects(0)
                    .set('Accept', 'application/json')
                    .end(function (res) {
                        var location = res.header['location'];
                        if (location) {
                            var lastSlash = location.lastIndexOf(channel.name);
                            var substring = location.substring(lastSlash + channel.name.length + 1).substring(0, 20);
                            console.log('history', substring)
                            channel.start = moment(substring, "YYYY/MM/DD/HH/mm/ss");
                        }
                        callback();
                    })
            }, function (err) {
                done(err);
            });

    }, timeout);

    function add(rootUrl, type) {
        channelTimes.push({
            source: rootUrl + '?location=CACHE&trace=true',
            compare: rootUrl + '?location=LONG_TERM_' + type + '&trace=true'
        });
    }

//    jasmine-node --forceexit --captureExceptions --config hubUrl hub.iad.dev.flightstats.io --config startOffset 48 verify_s3_writer_spec.js
//    jasmine-node --forceexit --captureExceptions --config hubUrl hub.iad.dev.flightstats.io verify_s3_writer_spec.js

    it('cross product of channels and times', function () {
        console.log('now', moment.utc().format(minute_format));
        console.log('startOffset', startOffset);
        console.log('endOffset', endOffset);
        for (var i = startOffset; i <= endOffset; i++) {
            channels.forEach(function (channel) {
                var start = channel.start.subtract(i, 'minutes');
                var formatted = start.format(minute_format);
                if (_.startsWith(channel.name, 'test')
                    || _.startsWith(channel.name, 'verifyMaxItems')
                    || Math.random() * 100 > testPercent) {
                    //do nothing
                } else {
                    var rootUrl = channel.href + formatted;
                    if (channel.storage == 'BOTH') {
                        add(rootUrl, 'SINGLE');
                        add(rootUrl, 'BATCH');
                    } else {
                        add(rootUrl, channel.storage);
                    }
                }
            });
        }
    }, timeout);


    it('compares query results', function (done) {
        async.eachLimit(channelTimes, 5,
            function (channelTime, callback) {
                console.log('calling', channelTime);
                async.parallel([
                        function (callback) {
                            agent
                                .get(channelTime.source)
                                .set('Accept', 'application/json')
                                .end(function (res) {
                                    expect(res.error).toBe(false);
                                    if (!res.body._links) {
                                        console.log('unable to find cache links', res.status, channelTime.source, res.body);
                                        callback(null, []);
                                    } else {
                                        callback(null, res.body._links.uris);
                                    }
                                });
                        },
                        function (callback) {
                            agent
                                .get(channelTime.compare)
                                .set('Accept', 'application/json')
                                .end(function (res) {
                                    expect(res.error).toBe(false);
                                    if (!res.body._links) {
                                        console.log('unable to find long term links', res.status, channelTime.compare, res.body);
                                        callback(null, []);
                                    } else {
                                        callback(null, res.body._links.uris);
                                    }
                                });
                        }
                    ],
                    function (err, results) {
                        var expected = results[0].length;
                        var actual = results[1].length;
                        if (expected > actual) {
                            console.log('failed ' + channelTime.compare + ' source=' + expected + ' compare=' + actual);
                            expect(actual).toBe(expected);
                        } else {
                            console.log('completed ' + channelTime.compare + ' with ' + expected);
                        }

                        callback(err);
                    });

            }, function (err) {
                done(err);
            });

    }, 2 * timeout);


});
