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

    it('loads replicated channels for specific domain', function (done) {
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

    var channels = [];

    it('gets replicated items', function (done) {
        //todo - gfm - 1/6/15 - do this with async
        replicatedDomain.excludeExcept.forEach(function (channel) {
            console.log('replicated', channel);
            agent.get(hubUrl + '/channel/' + channel + '/time/hour')
                .set('Accept', 'application/json')
                .end(function (res) {
                    expect(res.error).toBe(false);
                    channels.push({'name': channel, uris: res.body._links.uris});
                    done();
                })

        });
    }, 60 * 1000);

    function getSequence(uri) {
        return uri.substring(uri.lastIndexOf('/') + 1);
    }

    var itemsToVerify = [];

    it('checks for sequential', function (done) {
        channels.forEach(function (channel) {
            console.log('channel', channel);
            channel.verify = [];
            var sequence;
            channel.uris.forEach(function (uri) {
                if (sequence) {
                    var next = getSequence(uri);
                    expect(1 + parseInt(sequence)).toBe(parseInt(next));
                    sequence = next;
                    if (Math.random() > 0.99) {
                        itemsToVerify.push({name: channel.name, sequence: sequence, uri: uri});
                    }
                } else {
                    sequence = getSequence(uri);
                    itemsToVerify.push({name: channel.name, sequence: sequence, uri: uri});
                }
            });

            console.log('channel uris', channel.uris.length);
            console.log('channel verify', itemsToVerify.length);
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
                        console.log(item.uri + ' ' + results[0].length + ' ' + results[1].length);
                        expect(results[1].length).toBe(results[0].length);
                        callback(err);
                    });

            }, function (err) {
                done(err);
            });
    }, 30 * 60 * 1000);

});
