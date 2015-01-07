var agent = require('superagent');
var request = require('request');
var async = require('async');
var moment = require('moment');
var testName = __filename;
var hubUrl = process.env.hubUrl || 'hub-v2.svc.dev';
hubUrl = 'http://' + hubUrl;
console.log(hubUrl);

var sourceDomain = process.env.sourceDomain || 'hub.svc.prod';

describe(testName, function () {

    var replicatedDomain;

    it('loads ' + hubUrl + 'replicated channels for source ' + sourceDomain, function (done) {
        agent.get(hubUrl + '/replication')
            .set('Accept', 'application/json')
            .end(function (res) {
                expect(res.error).toBe(false);
                var domains = res.body.domains;
                console.log('domains', domains);
                domains.forEach(function (domain) {
                    if (domain.domain === sourceDomain) {
                        console.log('found', domain);
                        replicatedDomain = domain;
                    }
                });
                expect(replicatedDomain).not.toBeUndefined();
                done();
            })
    });

    var latestSourceItems = [];

    it('gets latest items for source channels', function (done) {
        async.eachLimit(replicatedDomain.excludeExcept, 40,
            function (channel, callback) {
                agent.get('http://' + sourceDomain + '/channel/' + channel + '/latest')
                    .set('Accept', 'application/json')
                    .redirects(0)
                    .end(function (res) {
                        expect(res.error).toBe(false);
                        latestSourceItems.push({name: channel, location: res.header['location']});
                        callback(res.error);
                    });
            }, function (err) {
                console.log('latest', latestSourceItems);
                done(err);
            });
    }, 2 * 60 * 1000);

    var channels = [];

    it('gets lists of replicated items', function (done) {
        async.eachLimit(replicatedDomain.excludeExcept, 20,
            function (channel, callback) {
                agent.get(hubUrl + '/channel/' + channel + '/time/hour?stable=false')
                    .set('Accept', 'application/json')
                    .end(function (res) {
                        expect(res.error).toBe(false);
                        channels.push({'name': channel, uris: res.body._links.uris});
                        callback(res.error);
                    });
            }, function (err) {
                done(err);
            });
    }, 60 * 1000);

    function getSequence(uri) {
        return parseInt(uri.substring(uri.lastIndexOf('/') + 1));
    }

    var itemsToVerify = [];

    it('makes sure replicated items are sequential ', function (done) {
        channels.forEach(function (channel) {
            channel.verify = [];
            var sequence;
            channel.uris.forEach(function (uri) {
                if (sequence) {
                    var next = getSequence(uri);
                    if (sequence + 1 !== next) {
                        console.log('wrong order ' + channel.name + ' expected ' + sequence + ' found ' + next);
                    }
                    expect(1 + sequence).toBe(next);
                    sequence = next;
                    if (Math.random() > 0.99) {
                        itemsToVerify.push({name: channel.name, sequence: sequence, uri: uri});
                    }
                } else {
                    sequence = getSequence(uri);
                    itemsToVerify.push({name: channel.name, sequence: sequence, uri: uri});
                }
            });

            console.log('uris for ', channel.name, channel.uris.length, itemsToVerify.length);
        });
        done();
    }, 5 * 1000);

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
                            getItem(item.uri, callback);
                        },
                        function (callback) {
                            getItem('http://' + sourceDomain + '/channel/' + item.name + "/" + item.sequence, callback);
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
    }, 30 * 60 * 1000);

    it('checks replicated for latest from source', function (done) {
        async.eachLimit(latestSourceItems, 40,
            function (item, callback) {
                agent.get(hubUrl + '/channel/' + item.name + '/latest?stable=false')
                    .set('Accept', 'application/json')
                    .redirects(0)
                    .end(function (res) {
                        expect(res.error).toBe(false);
                        var replicatedSequence = getSequence(res.header['location']);
                        var sourceSequence = getSequence(item.location);
                        if (replicatedSequence < sourceSequence) {
                            console.log('name ' + item.name + ' repl ' + replicatedSequence + ' source ' + sourceSequence);
                        }
                        expect(replicatedSequence).not.toBeLessThan(sourceSequence);
                        callback(res.error);
                    });
            }, function (err) {
                done(err);
            });
    }, 2 * 60 * 1000);



});
