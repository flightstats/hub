var agent = require('superagent');
var request = require('request');
var async = require('async');
var moment = require('moment');
var testName = __filename;
var hubUrl = process.env.hubUrl || 'hub-v2.svc.dev';
hubUrl = 'http://' + hubUrl;
console.log(hubUrl);

var sourceDomain = process.env.sourceDomain || 'hub.svc.prod';
var MINUTE = 60 * 1000;

describe(testName, function () {

    var replicatedChannelUrls = [];
    var replicatedChannels = {};

    it('loads ' + hubUrl + ' replicated channels from ' + sourceDomain, function (done) {
        agent.get(hubUrl + '/tag/replicated')
            .set('Accept', 'application/json')
            .end(function (res) {
                expect(res.error).toBe(false);
                var channels = res.body._links.channels;
                console.log('found replicated channels', channels);
                channels.forEach(function (channel) {
                    if (channel.name.substring(0, 4) !== 'test') {
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

    it('gets replication sources ', function (done) {
        async.eachLimit(replicatedChannelUrls, 20,
            function (channel, callback) {
                console.log('get channel', channel);
                agent.get(channel)
                    .set('Accept', 'application/json')
                    .end(function (res) {
                        expect(res.error).toBe(false);
                        replicatedChannels[channel]['replicationSource'] = res.body.replicationSource;
                        console.log('replicatedChannels', replicatedChannels);
                        callback(res.error);
                    });
            }, function (err) {
                done(err);
            });
    }, MINUTE);

    var channels = {};

    it('gets lists of replicated items', function (done) {
        async.eachLimit(replicatedChannelUrls, 20,
            function (channel, callback) {
                agent.get(channel + '/time/hour?stable=false')
                    .set('Accept', 'application/json')
                    .end(function (res) {
                        expect(res.error).toBe(false);
                        channels[channel] = res.body._links.uris;
                        agent.get(res.body._links.previous.href)
                            .set('Accept', 'application/json')
                            .end(function (res) {
                                expect(res.error).toBe(false);
                                channels[channel] = res.body._links.uris.concat(channels[channel]);
                                callback(res.error);
                            });
                    });
            }, function (err) {
                done(err);
            });
    }, MINUTE);

    function getSequence(uri) {
        return parseInt(uri.substring(uri.lastIndexOf('/') + 1));
    }

    var itemsToVerify = [];

    it('makes sure replicated items are sequential ', function (done) {
        for (var channel in channels) {
            var sequence = 0;
            console.log('working on', channel);
            channels[channel].forEach(function (uri) {
                if (sequence) {
                    var next = getSequence(uri);
                    if (sequence + 1 !== next) {
                        console.log('wrong order ' + channel + ' expected ' + sequence + ' found ' + next);
                    }
                    expect(1 + sequence).toBe(next);
                    sequence = next;
                    if (Math.random() > 0.99) {
                        itemsToVerify.push({name: channel, sequence: sequence, uri: uri});
                    }
                } else {
                    sequence = getSequence(uri);
                    itemsToVerify.push({name: channel, sequence: sequence, uri: uri});
                }
            });

            console.log(channels[channel].length + ' items for ' + channel + ' verify ' + itemsToVerify.length);
        }
        done();
    }, MINUTE);

    it('compares replicated items to source items', function (done) {
        function getItem(uri, callback) {
            request(uri, function (error, response, body) {
                expect(error).toBe(null);
                expect(response.statusCode).toBe(200);
                callback(null, body);
            });
        }

        async.eachLimit(itemsToVerify, 50,
            function (item, callback) {
                async.parallel([
                        function (callback) {
                            getItem(replicatedChannels[item.name].replicationSource + "/" + item.sequence, callback);
                        },
                        function (callback) {
                            getItem(item.uri, callback);
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

    //todo - gfm - 1/22/15 - come up with a better solution for this
    /*it('checks replicated for latest from source', function (done) {
        async.eachLimit(latestSourceItems, 40,
            function (item, callback) {
                agent.get(hubUrl + '/channel/' + item.name + '/latest?stable=false')
                    .set('Accept', 'application/json')
                    .redirects(0)
                    .end(function (res) {
                        expect(res.error).toBe(false);
                        if (res.statusCode !== 303) {
                            console.log('!!wrong status code!! ', item.name, res.statusCode);
                            expect(res.statusCode).toBe(303);
                        } else {
                            var replicatedSequence = getSequence(res.header['location']);
                            var sourceSequence = getSequence(item.location);
                            if (replicatedSequence < sourceSequence) {
                                console.log('name ' + item.name + ' repl ' + replicatedSequence + ' source ' + sourceSequence);
                            }
                            expect(replicatedSequence).not.toBeLessThan(sourceSequence);
                        }
                        callback(res.error);
                    });
            }, function (err) {
                done(err);
            });
     }, 2 * MINUTE);*/



});
