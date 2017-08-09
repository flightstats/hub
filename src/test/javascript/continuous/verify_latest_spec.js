require('../integration_config');
var agent = require('superagent');
var request = require('request');
var async = require('async');
var moment = require('moment');
var _ = require('lodash');
var testName = __filename;
var hubUrl = process.env.hubUrl;
hubUrl = 'http://' + hubUrl;
console.log(hubUrl);

var MINUTE = 60 * 1000;
const NONE = '1970/01/01/00/00/00/001/none';

/**
 * This should :
 * 1 - Get /ChannelLatestUpdated list from the hub
 * 2 - Read values for each channel
 * 3 - Get /latest values for each channel
 * 4 - Compare:
 *      if ZK value is NONE, channel should return 404
 *      if latest is not equal to zk, check zk again & compare
 *      check if ZK value is within Spoke cache
 *
 * skip global channels
 *
 */

//jasmine-node --forceexit --captureExceptions --config hubUrl hub verify_latest_spec.js

describe(testName, function () {

    var channelLastUpdated = hubUrlBase + '/internal/zookeeper/ChannelLatestUpdated';

    var zkChannels;
    it('1 - loads channels from ZooKeeper cache', function (done) {
        console.log('channelLastUpdated', channelLastUpdated);
        agent
            .get(channelLastUpdated)
            .end(function (err, res) {
                expect(err).toBe(null);
                expect(res.status).toBe(200);
                zkChannels = res.body.children;
                console.log('body', res.body);
                done();
            })
    }, MINUTE);

    var values = {};
    it('2 - reads values from ZooKeeper cache', function (done) {
        async.eachLimit(zkChannels, 10,
            function (zkChannel, callback) {
                console.log('get channel', zkChannel);
                agent.get(zkChannel)
                    .set('Accept', 'application/json')
                    .end(function (res) {
                        expect(res.error).toBe(false);
                        var name = zkChannel.substring(channelLastUpdated.length + 1);
                        if (name.substring(0, 4).toLowerCase() !== 'test') {
                            values[name] = {
                                zkKey: res.body.data.string,
                                stats: res.body.stats,
                                zkChannel: zkChannel
                            };
                            console.log('zk value', name, values[name].zkKey);
                        }
                        callback();
                    });
            }, function (err) {
                done(err);
            });

    }, MINUTE);

    it('3 - gets /latest', function (done) {
        async.forEachOfLimit(values, 10,
            function (item, name, callback) {
                console.log('get latest ', name);
                agent.get(channelUrl + '/' + name + '/latest?trace=true')
                    .set('Accept', 'application/json')
                    .redirects(0)
                    .end(function (res) {
                        if (res.status == 404) {
                            item['empty'] = true;
                        } else if (res.status == 303) {
                            expect(res.error).toBe(false);
                            var location = res.header['location'];
                            var latestKey = location.substring(channelUrl.length + name.length + 2);
                            console.log('latestKey ', latestKey, location);
                            item['latestKey'] = latestKey;
                        } else {
                            console.log('unexpected result', res);
                        }
                        callback();
                    });
            }, function (err) {
                done(err);
            });

    }, MINUTE);

    it('4 - verifies', function (done) {
        async.forEachOfLimit(values, 10,
            function (item, name, callback) {
                console.log('get latest ', name);

                if (item['empty']) {
                    agent.get(item.zkChannel)
                        .set('Accept', 'application/json')
                        .end(function (res) {
                            expect(res.error).toBe(false);
                            console.log('comparing to NONE ', name, res.body.data.string)
                            expect(res.body.data.string).toBe(NONE);
                            callback();
                        });
                } else {
                    expect(item.latestKey).toBeDefined();
                    if (item.latestKey !== item.zkKey) {
                        console.log('doing comparison on ', item);
                        agent.get(item.zkChannel)
                            .set('Accept', 'application/json')
                            .end(function (res) {
                                if (res.status == 404) {
                                    expect(isWithinSpokeWindow(item.latestKey)).toBe(true);
                                    console.log('missing zk value for ' + name);
                                } else {
                                    expect(res.error).toBe(false);
                                    expect(res.body.data.string).toBe(item.latestKey);
                                }
                                callback();
                            });
                    } else {
                        expect(item.zkKey).toBe(item.latestKey);
                        var withinSpokeWindow = isWithinSpokeWindow(item.latestKey);
                        if (withinSpokeWindow) {
                            console.log('comparing ', item);
                        }
                        expect(withinSpokeWindow).toBe(false);
                        callback();
                    }
                }

            }, function (err) {
                done(err);
            });

    }, MINUTE);

    function isWithinSpokeWindow(key) {
        var oneHourAgo = moment().utc().subtract(1, 'hours');
        var timePart = key.substring(0, key.lastIndexOf('/'));
        var keyTime = moment.utc(timePart, "YYYY/MM/DD/HH/mm/ss/SSS");
        console.log("key " + key + " time " + keyTime.format() + " " + oneHourAgo.format());
        return oneHourAgo.isBefore(keyTime);
    }


});
