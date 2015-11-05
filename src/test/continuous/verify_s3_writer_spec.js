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
var startOffset = process.env.startOffset || 48;
var endOffset = process.env.endOffset || 59;

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
                channels = res.body._links.channels;
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
                        callback();
                    })
            }, function (err) {
                done(err);
            });

    }, timeout);

    it('cross product of channels and times', function () {
        console.log('now', moment.utc().format(minute_format));
        console.log('startOffset', startOffset);
        console.log('endOffset', endOffset);
        for (var i = startOffset; i <= endOffset; i++) {
            var start = moment.utc().subtract(i, 'minutes');
            var formatted = start.format(minute_format);
            console.log('checking', formatted);
            channels.forEach(function (channel) {
                if (!_.startsWith(channel.name, 'test') && !_.startsWith(channel.name, 'verifyMaxItems')) {
                    var rootUrl = channel.href + formatted;
                    if (channel.storage == 'SINGLE') {
                        channelTimes.push({
                            source: rootUrl + '?location=CACHE',
                            compare: rootUrl + '?location=LONG_TERM_SINGLE'
                        });
                    } else if (channel.storage == 'BOTH') {
                        channelTimes.push({
                            source: rootUrl + '?location=CACHE',
                            compare: rootUrl + '?location=LONG_TERM_SINGLE'
                        });
                        channelTimes.push({
                            source: rootUrl + '?location=CACHE',
                            compare: rootUrl + '?location=LONG_TERM_BATCH'
                        });
                    } else if (channel.storage == 'BATCH') {
                        channelTimes.push({
                            source: rootUrl + '?location=CACHE',
                            compare: rootUrl + '?location=LONG_TERM_BATCH'
                        });
                    }
                }
            });
        }
    }, timeout);


    it('compares query results', function (done) {
        async.eachLimit(channelTimes, 10,
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

    }, timeout);


});
