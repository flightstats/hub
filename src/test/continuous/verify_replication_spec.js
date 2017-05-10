require('../integration/integration_config.js');
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

describe(testName, function () {

    //contains links to destination channels
    var replicatedChannelUrls = [];
    //uses key of replicationSource to channel body
    var replicatedChannels = {};

    it('loads ' + hubUrl + ' replicated channels ', function (done) {
        agent.get(hubUrl + '/tag/replicated')
            .set('Accept', 'application/json')
            .end(function (res) {
                expect(res.error).toBe(false);
                var channels = res.body._links.channels;
                console.log('found replicated channels', channels);
                channels.forEach(function (channel) {
                    if (channel.name.substring(0, 4) !== 'test') {
                        console.log('adding channel ', channel.href);
                        replicatedChannelUrls.push(channel.href);
                        replicatedChannels[channel.href] = channel;
                    } else {
                        console.log('excluding channel ', channel.name);
                    }
                });
                expect(replicatedChannelUrls.length).not.toBe(0);
                done();
            })
    }, MINUTE);

    var validReplicatedChannelUrls = [];

    it('gets replication sources ', function (done) {
        async.eachLimit(replicatedChannelUrls, 20,
            function (channel, callback) {
                console.log('get channel', channel);
                agent.get(channel)
                    .set('Accept', 'application/json')
                    .end(function (res) {
                        expect(res.error).toBe(false);
                        replicatedChannels[channel]['replicationSource'] = res.body.replicationSource;
                        agent.get(res.body.replicationSource)
                            .set('Accept', 'application/json')
                            .end(function (res) {
                                if (res.statusCode >= 400) {
                                    console.log('channel is missing remote source ', channel, res.body.replicationSource);
                                    callback();
                                } else {
                                    expect(res.error).toBe(false);
                                    validReplicatedChannelUrls.push(channel);
                                    callback(res.error);
                                }
                            });
                    });
            }, function (err) {
                done(err);
            });
    }, MINUTE);

    var channels = {};

    it('gets lists of replicated items', function (done) {
        async.eachLimit(validReplicatedChannelUrls, 20,
            function (channel, callback) {
                agent.get(channel + '/time/hour?stable=false&trace=true')
                    .set('Accept', 'application/json')
                    .end(function (res) {
                        expect(res.error).toBe(false);
                        channels[channel] = [];
                        console.log("hours", res.body);
                        agent.get(res.body._links.previous.href)
                            .set('Accept', 'application/json')
                            .end(function (res) {
                                expect(res.error).toBe(false);
                                channels[channel] = res.body._links.uris.concat(channels[channel]);
                                console.log('found dest second ', channels[channel][0]);
                                callback(res.error);
                            });
                    });
            }, function (err) {
                done(err);
            });
    }, 5 * MINUTE);

    it('verifies number of replicated items', function (done) {
        async.eachLimit(validReplicatedChannelUrls, 20,
            function (channel, callback) {
                var source = replicatedChannels[channel]['replicationSource'];
                agent.get(source + '/time/hour?stable=false')
                    .set('Accept', 'application/json')
                    .end(function (res) {
                        expect(res.error).toBe(false);
                        var uris = [];
                        agent.get(res.body._links.previous.href)
                            .set('Accept', 'application/json')
                            .end(function (res) {
                                expect(res.error).toBe(false);
                                uris = res.body._links.uris.concat(uris);
                                if (uris.length !== channels[channel].length) {
                                    console.log('unequal lengths ', channel, uris.length, channels[channel].length);
                                    logDifference(uris, channels[channel]);
                                }
                                expect(channels[channel].length).toBe(uris.length);
                                callback(res.error);
                            });
                    });
            }, function (err) {
                done(err);
            });
    }, MINUTE);

    function logDifference(source, destination) {
        if (source.length == 0 || destination.length == 0) {
            console.log('source ', source);
            console.log('destination', destination);
            return;
        }
        if (source.length + destination.length > 10000) {
            console.log("too many items to compare ", source.length, destination.length);
            return;
        }
        var sourceItems = mapContentKey(source, source[0].split('/')[4]);
        var destinationItems = mapContentKey(destination, destination[0].split('/')[4]);
        console.log('missing from source:', _.difference(sourceItems, destinationItems));
        console.log('missing from destination:', _.difference(destinationItems, sourceItems));
    }

    function mapContentKey(uris, channel) {
        return _.map(uris, function (value) {
            return getContentKey(value, channel);
        });
    }

    function getContentKey(uri, channel) {
        return uri.substring(uri.lastIndexOf(channel) + channel.length);
    }

    var itemsToVerify = [];

    it('select some random items for content verification ', function (done) {
        for (var channel in channels) {
            console.log('working on', channel);
            channels[channel].forEach(function (uri) {
                var replicatedChannel = replicatedChannels[channel];
                if (Math.random() > 0.99) {
                    var contentKey = getContentKey(uri, channel);
                    itemsToVerify.push({name: channel, uri: uri, contentKey: contentKey});
                }
            });

            console.log(channels[channel].length + ' items for ' + channel + ' verify ' + itemsToVerify.length);
        }
        done();
    }, MINUTE);

    it('compares replicated items to source items', function (done) {

        async.eachLimit(itemsToVerify, 50,
            function (item, callback) {
                async.parallel([
                        function (callback) {
                            utils.getItem(replicatedChannels[item.name].replicationSource + item.contentKey, callback);
                        },
                        function (callback) {
                            utils.getItem(item.uri, callback);
                        }
                    ],
                    function (err, results) {
                        var itemZero = results[0].length;
                        var itemOne = results[1].length;
                        if (itemOne !== itemZero) {
                            console.log('wrong length for item ' + item.uri + ' expected ' + itemZero + ' found ' + itemOne);
                        }
                        expect(itemOne).toBe(itemZero);
                        callback(err);
                    });

            }, function (err) {
                done(err);
            });
    }, 30 * MINUTE);

});
