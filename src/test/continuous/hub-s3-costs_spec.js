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

    function processData(datapoints) {
        var days = [];
        var sum = 0;
        for (var i = 0; i < datapoints.length; i++) {
            var point = datapoints[i];
            if (point[0]) {
                days.push(point[0]);
                sum += point[0];
            } else {
                days.push(0);
            }
        }
        return {sum: sum, days: days};
    }

    function addData(channel, field, value, data) {
        if (value.target.indexOf(field.target) > 0) {
            channel[field.name] = data.sum;
            channel[field.name + 'Days'] = data.days;
        }
    }

    function addIfMissing(channel, fieldName) {
        if (!channel[fieldName]) {
            channel[fieldName] = 0;
            channel[fieldName + 'Days'] = [0, 0, 0, 0, 0, 0, 0];
        }
    }

    var graphiteData = [];
    var days = 7;
    var start = moment().utc().hours(0).minutes(0).seconds(0).milliseconds(0).subtract(days, 'days').format("X");

    function createField(name, target) {
        return {name: name, target: target};
    }

    it('gets hosted graphite information ', function (done) {
        var end = moment().utc().hours(0).minutes(0).seconds(0).milliseconds(0).subtract(1, 'seconds').format("X");
        var options = '"1days","sum",alignToFrom=true)';
        async.eachLimit(channelData, 20,
            function (channel, callback) {
                fields = [
                    createField('put', 's3.put'),
                    createField('putBatch', 's3Batch.put'),
                    createField('list', 's3.list'),
                    createField('listBatch', 's3Batch.list'),
                    createField('get', 's3.get'),
                    createField('getBatch', 's3Batch.get'),
                    createField('bytes', 's3.bytes'),
                    createField('bytesBatch', 's3Batch.bytes')
                ];
                var url = graphiteUrl + 'render?format=json';
                fields.forEach(function (field) {
                    url += '&target=summarize(' + dataPrefix + '.channel.' + channel.name +
                        '.' + field.target + ':sum,' + options;
                })
                url += '&from=' + start + '&until=' + end;

                console.log('get data ', url);
                request.get({url: url},
                    function (err, response, body) {
                        expect(err).toBeNull();
                        console.log('channel ', channel.name, body);
                        if (err !== null || response.statusCode !== 200) {
                            return callback(err);
                        }
                        expect(response.statusCode).toBe(200);
                        var parsed = JSON.parse(body);
                        for (var i = 0; i < parsed.length; i++) {
                            var value = parsed[i];
                            var data = processData(value.datapoints);
                            fields.forEach(function (field) {
                                addData(channel, field, value, data);
                            })
                        }
                        fields.forEach(function (field) {
                            addIfMissing(channel, field.name);
                        })
                        graphiteData.push(channel);
                        callback(err);
                    });

            }, function (err) {
                done(err);
            });
    }, 20 * MINUTE);

    var output = [];

    /**
     * change to new graphite values
     *
     */
    it('creates output items ', function (done) {
        async.eachLimit(graphiteData, 20,
            function (channel, callback) {
                var event = {};
                output.push(event);
                event.name = channel.name;
                event.hub = dataPrefix;

                event.MonthlyS3PutCost = channel.put / days / 1000 * 0.005 * 30;
                event.MonthlyS3PutBatchCost = channel.putBatch / days / 1000 * 0.005 * 30;
                event.MonthlyS3ListCost = channel.list / days / 1000 * 0.005 * 30;
                event.MonthlyS3ListBatchCost = channel.listBatch / days / 1000 * 0.005 * 30;
                event.MonthlyS3GetCost = channel.get / days / 10000 * 0.004 * 30;
                event.MonthlyS3GetBatchCost = channel.getBatch / days / 10000 * 0.004 * 30;
                event.MonthlyBytesCost = channel.bytes / days / 1024 / 1024 / 1024 * 0.03 * channel.earliest;
                event.MonthlyBytesBatchCost = channel.bytesBatch / days / 1024 / 1024 / 1024 * 0.03 * channel.earliest;
                event.ProjectedBytesCost = (channel.bytes + channel.bytesBatch) / days / 1024 / 1024 / 1024 * 0.03 * channel.hub.ttlDays;
                event.MonthlyTotalCost = event.MonthlyS3PutCost + event.MonthlyS3ListCost + event.MonthlyS3GetCost + event.MonthlyBytesCost +
                    event.MonthlyS3PutBatchCost + event.MonthlyS3ListBatchCost + event.MonthlyS3GetBatchCost + event.MonthlyBytesBatchCost;
                event.earliestItemDays = channel.earliest;
                event.tags = channel.hub.tags;
                event.ttlDays = channel.hub.ttlDays;
                event.owner = channel.hub.owner;
                event.days = [];
                for (var i = 0; i < days; i++) {
                    console.log('channel', channel);
                    event.days.push({
                        s3Put: channel.putDays[i] + channel.putBatchDays[i],
                        s3List: channel.listDays[i] + channel.listBatchDays[i],
                        s3Get: channel.getDays[i] + channel.getBatchDays[i],
                        s3Bytes: channel.bytesDays[i] + channel.bytesBatchDays[i],
                    });
                }

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
