var agent = require('superagent');
var request = require('request');
var async = require('async');
var moment = require('moment');
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
                        agent.get(res.body.replicationSource)
                            .set('Accept', 'application/json')
                            .end(function (res) {
                                if (res.statusCode >= 400) {
                                    console.log('channel is missing remote source ', channel, res.body.replicationSource);
                                    callback();
                                } else {
                                    expect(res.error).toBe(false);
                                    var server = res.header['server'];
                                    console.log('server', server);
                                    if (server.indexOf('Hub') >= 0) {
                                        replicatedChannels[channel]['version'] = 'v2';
                                    } else {
                                        replicatedChannels[channel]['version'] = 'v1';
                                    }
                                    console.log('version', channel, replicatedChannels[channel]['version']);
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
        async.eachLimit(replicatedChannelUrls, 20,
            function (channel, callback) {
                agent.get(channel + '/time/hour?stable=false')
                    .set('Accept', 'application/json')
                    .end(function (res) {
                        expect(res.error).toBe(false);
                        channels[channel] = res.body._links.uris;
                        console.log('found dest first ', channels[channel][0]);
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

    it('verifies v2 replicated items', function (done) {

        async.eachLimit(replicatedChannelUrls, 20,
            function (channel, callback) {
                if (replicatedChannels[channel]['version'] !== 'v2') {
                    callback();
                    return;
                }
                var source = replicatedChannels[channel]['replicationSource'];
                agent.get(source + '/time/hour?stable=false')
                    .set('Accept', 'application/json')
                    .end(function (res) {
                        expect(res.error).toBe(false);
                        var v2Uris = res.body._links.uris;
                        console.log('found source first ', v2Uris[0]);
                        agent.get(res.body._links.previous.href)
                            .set('Accept', 'application/json')
                            .end(function (res) {
                                expect(res.error).toBe(false);
                                v2Uris = res.body._links.uris.concat(v2Uris);
                                var foundFirst = 0;
                                var foundLast = 0;
                                if (v2Uris.length !== channels[channel].length) {
                                    console.log('comparing length ', channel, v2Uris.length, channels[channel].length);
                                    var uri = channels[channel][0];
                                    if (uri) {
                                        var firstKey = getContentKey(uri, channel);
                                        var lastKey = getContentKey(channels[channel][channels[channel].length - 1], channel);
                                        for (var i = 0; i < v2Uris.length; i++) {
                                            var item = v2Uris[i];

                                            var sourceKey = getContentKey(item, source);
                                            if (sourceKey === firstKey) {
                                                foundFirst = i;
                                                console.log('found first ', channel, v2Uris.length, channels[channel].length, i);
                                            }
                                            if (sourceKey === lastKey) {
                                                foundLast = i;
                                                console.log('found last', channel, v2Uris.length, channels[channel].length, i);
                                            }
                                        }
                                        console.log('found ', v2Uris.length, channels[channel].length, foundFirst, foundLast);
                                        expect(channels[channel].length + foundFirst).toBe(foundLast + 1);
                                    } else {
                                        console.log("failure: expected to have uris", channel);
                                        expect(uri).toBeDefined();
                                    }

                                }
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

    function getContentKey(uri, channel) {
        return uri.substring(uri.lastIndexOf(channel) + channel.length);
    }

    var itemsToVerify = [];

    function checkForBlackHole(replicatedChannel, expected, channel, next) {
        agent.get(replicatedChannel.replicationSource + "/" + expected)
            .end(function (res) {
                if (res.statusCode == 404) {
                    //this means it's a black hole
                } else {
                    console.log('wrong order ' + channel + ' expected ' + expected + ' found ' + next);
                    expect(expected).toBe(next);
                }
            })
    }

    it('makes sure replicated items are sequential ', function (done) {

        for (var channel in channels) {
            var sequence = 0;
            console.log('working on', channel);
            channels[channel].forEach(function (uri) {
                var replicatedChannel = replicatedChannels[channel];
                if (replicatedChannel.version === 'v1') {
                    if (sequence) {
                        var next = getSequence(uri);
                        var expected = sequence + 1;
                        if (expected !== next) {
                            checkForBlackHole(replicatedChannel, expected, channel, next);
                        } else {
                            if (Math.random() > 0.99) {
                                itemsToVerify.push({name: channel, sequence: next, uri: uri});
                            }
                        }
                        sequence = next;

                    } else {
                        sequence = getSequence(uri);
                        itemsToVerify.push({name: channel, sequence: sequence, uri: uri});
                    }
                } else {
                    if (Math.random() > 0.99) {
                        var contentKey = getContentKey(uri, channel);
                        itemsToVerify.push({name: channel, sequence: false, uri: uri, contentKey: contentKey});
                    }
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
                if (response.statusCode !== 200) {
                    console.log('wrong status ', uri, response.statusCode);
                }
                expect(response.statusCode).toBe(200);
                callback(null, body);
            });
        }

        async.eachLimit(itemsToVerify, 50,
            function (item, callback) {
                async.parallel([
                        function (callback) {
                            if (item.sequence) {
                                getItem(replicatedChannels[item.name].replicationSource + "/" + item.sequence, callback);
                            } else {
                                getItem(replicatedChannels[item.name].replicationSource + item.contentKey, callback);
                            }
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
