require('../integration_config');
var async = require('async');
var moment = require('moment');
var testName = __filename;
var hubUrl = process.env.hubUrl;
hubUrl = 'http://' + hubUrl + '/channel';
console.log('hubUrl:' + hubUrl);

var timeout = 5 * 60 * 1000;
var testPercent = parseInt(process.env.testPercent) || 20;
console.log('testPercent: ' + testPercent);

/**
 * Usage:
 * jasmine-node --forceexit --captureExceptions --config hubUrl hub.dev --config testPercent 20 convert_single_spec.js
 *
 * This will convert a testPercent of the channels in a hub cluster from SINGLE to BOTH
 *
 */
describe(testName, function () {

    const storageSource = 'SINGLE';
    const storageDestination = 'BOTH';

    /*const storageSource = 'BOTH';
    const storageDestination = 'SINGLE';*/

    let channels = [];
    let converting = [];

    it('loads channels', function (done) {
        let headers = {'Accept': 'application/json'};
        utils.httpGet(hubUrl, headers)
            .then(res => {
                let allChannels = res.body._links.channels;
                allChannels.forEach(function (channel) {
                    console.log('channel', channel);
                    if (channel.name.substring(0, 4).toLowerCase() !== 'test') {
                        channels.push(channel);
                    }
                });
            })
            .catch(error => {
                expect(error).toBeNull();
            })
            .finally(done);
    }, timeout);

    it('loads channel data', function (done) {
        async.eachLimit(channels, 10,
            function (channel, callback) {
                console.log('calling', channel);
                let headers = {'Accept': 'application/json'};
                utils.httpGet(channel.href, headers)
                    .then(res => {
                        channel.storage = res.body.storage;
                        if (channel.storage === storageSource) {
                            console.log(storageSource + ' ', channel);
                            if (Math.random() * 100 <= testPercent) {
                                console.log('converting ', channel);
                                converting.push(channel);
                            }

                        }
                        callback();
                    })
                    .catch(error => {
                        expect(error).toBeNull();
                    });
            }, function (err) {
                done(err);
            });

    }, timeout);

    it('changes ' + storageSource + ' to ' + storageDestination, function (done) {
        async.eachLimit(converting, 20,
            function (channel, callback) {

                console.log('switching to BOTH ', channel);
                let url = `${channel.href}`;
                let headers = {'Accept': 'application/json'};
                let body = {storage: storageDestination};
                utils.httpPut(url, headers, body)
                    .then(res => {
                        console.log('success!  -> ', res.body);
                        callback();
                    })
                    .catch(error => {
                        expect(error).toBeNull();
                    });
            }, function (err) {
                done(err);
            });

    }, timeout);

});
