require('../integration/integration_config.js');

var agent = require('superagent');
var request = require('request');
var async = require('async');
var moment = require('moment');
var _ = require('lodash');
var testName = __filename;
var hubUrl = process.env.hubUrl;
console.log('hubUrl ' + hubUrl);
var dataPrefix = process.env.dataPrefix;
console.log('dataPrefix ' + dataPrefix);
var graphiteUrl = process.env.graphiteUrl;
console.log('graphiteUrl ' + graphiteUrl);

var MINUTE = 60 * 1000;

/**
 * This should :
 *
 * 1 - Download all the channels for a hub instance
 * 2 - for each channel
 *   A - Get data from graphite
 *   D - Get Tags & TTL days from Hub
 * 3- Put output into a channel for processing
 *
 */

//jasmine-node --forceexit --captureExceptions --config hubUrl http://hub.svc.dev --config dataPrefix hub.dev --config graphiteUrl https://www.hostedgraphite.com/ hub-s3-costs_spec.js

describe(testName, function () {

    //contains links to destination channels
    var channelData = [];

    it('loads ' + hubUrl + ' channels ', function (done) {
        agent.get(hubUrl + '/channel')
            .set('Accept', 'application/json')
            .end(function (res) {
                expect(res.error).toBe(false);
                var channels = res.body._links.channels;
                channels.forEach(function (channel) {
                    if (channel.name.substring(0, 4) !== 'test') {
                        channelData.push(channel);
                    }
                });
                expect(channelData.length).not.toBe(0);
                done();
            })
    }, MINUTE);

    it('gets channel information ', function (done) {
        async.eachLimit(channelData, 20,
            function (channel, callback) {
                agent.get(channel.href)
                    .set('Accept', 'application/json')
                    .end(function (res) {
                        expect(res.error).toBe(false);
                        channel.hub = res.body;
                        callback(res.error);
                    });
            }, function (err) {
                done(err);
            });
    }, MINUTE);

    var now = moment().utc();
    it('gets earliest information ', function (done) {
        async.eachLimit(channelData, 20,
            function (channel, callback) {
                agent.get(channel.href + '/earliest')
                    .redirects(0)
                    .end(function (res) {
                        if (res.status === 404) {
                            channel.earliest = 0;
                        } else if (res.status === 303) {
                            var location = res.header.location;
                            location = location.slice(0, location.lastIndexOf("/"));
                            location = location.slice(channel.href.length + 1);
                            var earliest = moment(location, "YYYY/MM/DD/HH/mm/ss/SSS");
                            channel.earliest = now.diff(earliest, 'days');
                        } else {
                            console.log('unexpected status for earliest', channel.name, res.status);
                            channel.earliest = 0;
                        }
                        callback();
                    });
            }, function (err) {
                done(err);
            });
    }, MINUTE);

    var graphiteData = [];
    var days = 7;
    var start = moment().utc().hours(0).minutes(0).seconds(0).milliseconds(0).subtract(days, 'days').format("X");
    it('gets hosted graphite information ', function (done) {
        var end = moment().utc().hours(0).minutes(0).seconds(0).milliseconds(0).subtract(1, 'seconds').format("X");

        async.eachLimit(channelData, 20,
            function (channel, callback) {
                var field = dataPrefix + '.channel.' + channel.name;
                var url = graphiteUrl + 'render?format=json' +
                    '&target=summarize(' + field + '.s3.requestA:sum,"7days","sum",alignToFrom=true)' +
                    '&target=summarize(' + field + '.s3.requestB:sum,"7days","sum",alignToFrom=true)' +
                    '&target=summarize(' + field + '.post.bytes:sum,"7days","sum",alignToFrom=true)' +
                    '&target=summarize(' + field + '.post:obvs,"7days","sum",alignToFrom=true)' +
                    '&from=' + start + '&until=' + end;

                console.log('get data ', url);
                request.get({url: url},
                    function (err, response, body) {
                        expect(err).toBeNull();
                        expect(response.statusCode).toBe(200);
                        console.log('channel ', channel.name, body);
                        var parsed = JSON.parse(body);
                        for (var i = 0; i < parsed.length; i++) {
                            var value = parsed[i];
                            if (value.target.indexOf('requestA') > 0) {
                                channel.requestA = value.datapoints[0][0];
                            } else if (value.target.indexOf('requestB') > 0) {
                                channel.requestB = value.datapoints[0][0];
                            } else if (value.target.indexOf('post.bytes') > 0) {
                                channel.bytes = value.datapoints[0][0];
                            } else if (value.target.indexOf('post:obvs') > 0) {
                                channel.posts = value.datapoints[0][0];
                            }
                        }
                        if (!channel.requestA) {
                            channel.requestA = 0;
                        }
                        if (!channel.requestB) {
                            channel.requestB = 0;
                        }
                        if (!channel.bytes) {
                            channel.bytes = 0;
                        }
                        if (!channel.posts) {
                            channel.posts = 0;
                        }
                        graphiteData.push(channel);
                        callback(err);

                    });

            }, function (err) {
                done(err);
            });
    }, 10 * MINUTE);

    var output = [];

    it('creates output items ', function (done) {
        async.eachLimit(graphiteData, 20,
            function (channel, callback) {
                var event = {};
                output.push(event);
                event.name = channel.name;
                event.hub = dataPrefix;

                event.MonthlyS3PutCost = channel.posts / days / 1000 * 0.005 * 30;
                event.MonthlyS3ListCost = (channel.requestA - channel.posts) / days / 1000 * 0.005 * 30;
                event.MonthlyS3GetCost = channel.requestB / days / 10000 * 0.004 * 30;
                event.MonthlyBytesCost = channel.bytes / days / 1024 / 1024 / 1024 * 0.03 * channel.earliest;
                event.MonthlyTotalCost = event.MonthlyS3PutCost + event.MonthlyS3ListCost + event.MonthlyS3GetCost + event.MonthlyBytesCost;
                event.earliestItemDays = channel.earliest;

                callback();

            }, function (err) {
                done(err);
            });
    }, MINUTE);


    var hubChannel = hubUrl + '/channel/hub_s3_costs';

    it('creates hub channel for data', function (done) {
        console.log('creating ', hubChannel);
        agent
            .put(hubChannel)
            .accept('json')
            .send({"ttlDays": "30"})
            .end(function (err, res) {
                expect(err).toBe(null);
                done();
            })
    });

    it('posts data to hub channel ' + hubChannel, function (done) {
        console.log('posting ', hubChannel);
        agent
            .post(hubChannel)
            .accept('json')
            .send(output)
            //.send(output.slice(0, 50))
            .end(function (err, res) {
                expect(err).toBe(null);
                expect(res.status).toBe(201);
                console.log(res.header.location);
                done();
            })
    });

});
