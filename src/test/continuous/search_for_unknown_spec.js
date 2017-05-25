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
var channelInput = process.env.channels;
console.log(channelInput);

var MINUTE = 60 * 1000;

/**
 * This should:
 * Attempt to get known existing items from the hub which are outside the Spoke TTL.
 * Log any non-2xx responses as failures.
 *
 */
describe(testName, function () {

    var channels = ['verifier_test_1', 'verifier_test_2', 'verifier_test_3'];
    if (channelInput) {
        channels = channelInput.split(",");
    }
    console.log('channels', channels);

    var urisToVerify = [];

    it('runs day queries ', function (done) {
        async.eachLimit(channels, 2,
            function (channel, callback) {
                console.log('get channel', channel);
                agent.get(hubUrl + '/channel/' + channel + '/time/day')
                    .set('Accept', 'application/json')
                    .end(function (res) {
                        expect(res.error).toBe(false);
                        var uris = res.body._links.uris;
                        uris.forEach(function (uri) {
                            if (Math.random() > 0.995) {
                                urisToVerify.push(uri);
                            }
                        });
                        callback();
                    });
            }, function (err) {
                done(err);
            });
    }, 5 * MINUTE);

    var success = 0;

    it('verifies items', function (done) {
        console.log('looking at ' + urisToVerify.length + ' items ');
        async.eachLimit(urisToVerify, 50,
            function (uri, callback) {
                request({uri: uri, encoding: null},
                    function (error, response, body) {
                        if (error) {
                            console.log('got error ', uri, error);
                        } else {
                            if (response.statusCode === 200) {
                                success++
                            } else {
                                console.log('wrong status ', uri, response.statusCode);
                            }
                        }
                        callback(null, body);
                    });

            }, function (err) {
                done(err);
            });
    }, 30 * MINUTE);


    it('compares results', function () {
        console.log('looking at ' + urisToVerify.length + ' items with ' + success + ' success');
        expect(success).toBe(urisToVerify.length);
    });

});
