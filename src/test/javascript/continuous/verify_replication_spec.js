require('../integration_config');
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

    getItem = function getItem(uri, callback) {
        request({uri: uri, encoding: null}, function (error, response, body) {
            expect(error).toBe(null);
            if (error) {
                console.log('got error ', uri, error);
            } else {
                if (response.statusCode !== 200) {
                    console.log('wrong status ', uri, response.statusCode);
                }
                expect(response.statusCode).toBe(200);
            }
            callback(null, body);
        });
    };

    it('loads ' + hubUrl + ' replicated channels ', function (done) {
        let url = `${hubUrl}/tag/replicated`;
        let headers = {'Accept': 'application/json'};
        utils.httpGet(url, headers)
            .then(res => {
                var channels = res.body._links.channels;
                console.log('found replicated channels', channels);
                channels.forEach(channel => {
                    if (channel.name.substring(0, 4).toLowerCase() !== 'test') {
                        console.log('adding channel ', channel.href);
                        replicatedChannelUrls.push(channel.href);
                        replicatedChannels[channel.href] = channel;
                    } else {
                        console.log('excluding channel ', channel.name);
                    }
                });
                expect(replicatedChannelUrls.length).not.toBe(0);
            })
            .catch(error => expect(error).toBeNull())
            .finally(done);
    });

    var validReplicatedChannelUrls = [];

    it('gets replication sources ', function (done) {
        async.eachLimit(replicatedChannelUrls, 20,
            function (channel, callback) {
                console.log('get channel', channel);

                let url = channel;
                let headers = {'Accept': 'application/json'};
                utils.httpGet(url, headers)
                    .then(res => {
                        let url = res.body.replicationSource;
                        let headers = {'Accept': 'application/json'};
                        replicatedChannels[channel]['replicationSource'] = url;

                        return utils.httpGet(url, headers);
                    }).then(res => {
                        if (res.statusCode >= 400) {
                            console.log('channel is missing remote source ', channel, res.body.replicationSource);
                            callback();
                        } else {
                            validReplicatedChannelUrls.push(channel);
                            callback(res.error);
                        }
                    })
                    .catch(error => expect(error).toBeNull())
            }, function (err) {
                done(err);
            });
    }, MINUTE);

    var channels = {};

    it('gets lists of replicated items', function (done) {
        async.eachLimit(validReplicatedChannelUrls, 20, (channel, callback) => {
            let url = channel + '/time/hour?stable=false&trace=true';
            let headers = {'Accept': 'application/json'};

            utils.httpGet(url, headers)
                .then(utils.followRedirectIfPresent)
                .then(res => {
                    channels[channel] = [];
                    let url = res.body._links.previous.href
                    let headers = {'Accept': 'application/json'};
                    return utils.httpGet(url, headers)
                })
                .then(res => {
                    channels[channel] = res.body._links.uris.concat(channels[channel]);
                    console.log('found dest second ', channels[channel][0]);
                    callback(res.error);
                })
                .catch(error => expect(error).toBeNull())
        }, function (err) {
            done(err);
        });
    }, 5 * MINUTE);

    it('verifies number of replicated items', function (done) {
        let acceptHeader = {'Accept': 'application/json'};

        async.eachLimit(validReplicatedChannelUrls, 20, (channel, callback) => {
            var source = replicatedChannels[channel]['replicationSource'];
            let uris = [];

            utils.httpGet(`${source}/time/hour?stable=false`, acceptHeader)
                .then(utils.followRedirectIfPresent)
                .then(res => {
                    let url = res.body._links.previous.href;
                    return utils.httpGet(url, acceptHeader);
                })
                .then(res => {
                    uris = res.body._links.uris.concat(uris);
                    if (uris.length !== channels[channel].length) {
                        console.log('unequal lengths ', channel, uris.length, channels[channel].length);
                        logDifference(uris, channels[channel]);
                    }
                    expect(channels[channel].length).toBe(uris.length);
                    callback(res.error);
                })
                .catch(error => expect(error).toBeNull())
        }, function (err) {
            done(err);
        });
    });

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
        console.log('missing from source:', sourceItems.filter(sourceItem => !(sourceItem in destinationItems)));
        console.log('missing from destination:', destinationItems.filter(destinationItem => !(destinationItem in sourceItems)));
    }

    function mapContentKey(uris, channel) {
        return uris.map(function (value) {
            return getContentKey(value, channel);
        });
    }

    function getContentKey(uri, channel) {
        return uri.substring(uri.lastIndexOf(channel) + channel.length);
    }

    var itemsToVerify = [];

    it('select some random items for content verification ', function (done) {
        for (var channel in channels) {
            if (channel.toLowerCase().indexOf('large_test') > 0) {
                console.log('excluding', channel);
            } else {
                console.log('working on', channel);
                channels[channel].forEach(function (uri) {
                    var replicatedChannel = replicatedChannels[channel];
                    if (Math.random() > 0.99) {
                        var contentKey = getContentKey(uri, channel);
                        itemsToVerify.push({name: channel, uri: uri, contentKey: contentKey});
                    }
                });
            }

            console.log(channels[channel].length + ' items for ' + channel + ' verify ' + itemsToVerify.length);
        }
        done();
    }, MINUTE);

    it('compares replicated items to source items', function (done) {

        async.eachLimit(itemsToVerify, 50,
            function (item, callback) {
                async.parallel([
                        function (callback) {
                            getItem(replicatedChannels[item.name].replicationSource + item.contentKey, callback);
                        },
                        function (callback) {
                            getItem(item.uri, callback);
                        }
                    ],
                    function (err, results) {
                        if (results[0] && results[1]) {
                            var itemZero = results[0].length;
                            var itemOne = results[1].length;
                            if (itemOne !== itemZero) {
                                console.log('wrong length for item ' + item.uri + ' expected ' + itemZero + ' found ' + itemOne);
                            }
                            expect(itemOne).toBe(itemZero);

                        } else {
                            console.log('missing result for ' + item.name + ' ' + item.contentKey);
                            expect(results[0]).not.toBeNull();
                            expect(results[1]).not.toBeNull();
                        }
                        callback(err);

                    });

            }, function (err) {
                done(err);
            });
    }, 30 * MINUTE);

});
