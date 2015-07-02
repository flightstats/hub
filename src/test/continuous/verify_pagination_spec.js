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

/**
 * This assumes that a channel is being written to continuously.
 *
 * This should :
 * 1 - Get an hour's worth of data from two days ago.
 * 2 - Call previous on the first item
 * 3 - From the previous item, call next/N, and verify same set
 * Call next on the last item
 * From the next item, call previous/N , and verify same set
 *
 */

//jasmine-node --forceexit --captureExceptions --config hubUrl hub-v2.svc.dev verify_pagination_spec.js

describe(testName, function () {

    var channelUrl = hubUrl + '/channel/load_test_1';
    var twoDaysAgo = channelUrl + '/' + moment().utc().subtract(2, 'days').format('YYYY/MM/DD/HH/mm');

    var uris = [];

    it('1 - gets two days ago', function (done) {
        console.log('channelUrl', twoDaysAgo);
        agent
            .get(twoDaysAgo)
            .end(function (err, res) {
                expect(err).toBe(null);
                //console.log('res', res);
                expect(res.status).toBe(200);

                uris = res.body._links.uris;
                console.log('length', uris.length);
                done();
            })
    }, 60 * 1000);

    var previous = '';

    it('2 - gets previous ' + uris[0], function (done) {
        getLocation(uris[0] + '/previous', done, function (location) {
            previous = location;
        });
    }, 60 * 1001);

    it('3 - gets next N from ' + previous, function (done) {
        getAndCompare(previous + '/next/' + uris.length, done);
    }, 60 * 1002);

    var next = '';

    it('4 - gets next ' + uris[uris.length - 1], function (done) {
        getLocation(uris[uris.length - 1] + '/next', done, function (location) {
            next = location;
        });
    }, 60 * 1003);


    it('5 - gets previous N from ' + next, function (done) {
        getAndCompare(next + '/previous/' + uris.length, done);
    }, 60 * 1004);

    function getLocation(url, done, assign) {
        console.log('get location ', url);
        agent
            .get(url)
            .redirects(0)
            .end(function (err, res) {
                expect(err).toBe(null);
                expect(res.status).toBe(303);
                console.log('res.headers.location', res.headers.location);
                assign(res.headers.location);
                done();
            })
    }

    function getAndCompare(url, done) {
        console.log('gets ', url);
        agent
            .get(url)
            .end(function (err, res) {
                expect(err).toBe(null);
                expect(res.status).toBe(200);
                for (var i = 0; i < uris.length; i++) {
                    expect(res.body._links.uris[i]).toBe(uris[i]);
                }
                done();
            })
    }

});
