require('../integration_config');
var async = require('async');
var moment = require('moment');
var testName = __filename;
var hubUrl = process.env.hubUrl;
hubUrl = 'http://' + hubUrl + '/channel';
console.log(hubUrl);

var timeout = 5 * 60 * 1000;
var minute_format = '/YYYY/MM/DD/HH/mm';
var startOffset = parseInt(process.env.startOffset) || 29;
var endOffset = parseInt(process.env.endOffset) || 40;
var testPercent = parseInt(process.env.testPercent) || 10;

/**
 * Usage:
 * jasmine-node --forceexit --captureExceptions --config hubUrl hub --config startOffset 48 verify_s3_writer_spec.js
 *
 * This should load all the channels in the hub.
 * For each channel, verify that the items in S3 match the items in Spoke
 * testPercent is used to limit the cost of all the querys
 */
describe(testName, function () {

    var channels = [];
    var channelTimes = [];

    it('loads channels', function (done) {
        let headers = {'Accept': 'application/json'};
        utils.httpGet(hubUrl, headers)
            .then(res => {
                var allChannels = res.body._links.channels;
                allChannels.forEach(function (channel) {
                    console.log('channel', channel);
                    if (channel.name.substring(0, 4).toLowerCase() !== 'test') {
                        channels.push(channel);
                    }
                });
            })
            .finally(done);
    }, timeout);

    it('loads channel data', function (done) {
        async.eachLimit(channels, 10,
            function (channel, callback) {
                console.log('calling', channel);
                let headers = {'Accept': 'application/json'};
                utils.httpGet(channel.href, headers)
                    .then(res => {
                        channel.storage = res.body.storage;
                        channel.start = moment.utc();
                        if (res.body.historical) {
                            channel.history = true;
                        }
                        callback();
                    })
                    .catch(error => callback(error))
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
                let url = `${channel.href}/latest`;
                let headers = {'Accept': 'application/json'};
                utils.httpGet(url, headers)
                    .then(res => {
                        var location = res.header['location'];
                        if (location) {
                            var lastSlash = location.lastIndexOf(channel.name);
                            var substring = location.substring(lastSlash + channel.name.length + 1).substring(0, 20);
                            console.log('history', substring)
                            channel.start = moment(substring, "YYYY/MM/DD/HH/mm/ss");
                        }
                        callback();
                    })
                    .catch(error => callback(error))
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

    it('cross product of channels and times', function () {
        console.log('now', moment.utc().format(minute_format));
        console.log('startOffset', startOffset);
        console.log('endOffset', endOffset);
        for (var i = startOffset; i <= endOffset; i++) {
            channels.forEach(function (channel) {
                var start = channel.start.subtract(i, 'minutes');
                var formatted = start.format(minute_format);
                if (channel.name.toLowerCase().startsWith('test')
                    || channel.name.startsWith('verifyMaxItems')
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
                let headers = {'Accept': 'application/json'};
                async.parallel([
                        function (callback) {
                            utils.httpGet(channelTime.source, headers)
                                .then(res => {
                                    if (!res.body._links) {
                                        console.log('unable to find cache links', res.status, channelTime.source, res.body);
                                        callback(null, []);
                                    } else {
                                        callback(null, res.body._links.uris);
                                    }
                                })
                                .catch(error => callback(error))
                        },
                        function (callback) {
                            utils.httpGet(channelTime.compare, headers)
                                .then(res => {
                                    if (!res.body._links) {
                                        console.log('unable to find long term links', res.status, channelTime.compare, res.body);
                                        callback(null, []);
                                    } else {
                                        callback(null, res.body._links.uris);
                                    }
                                })
                                .catch(error => callback(error))
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
