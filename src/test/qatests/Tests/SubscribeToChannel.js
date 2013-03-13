var chai = require('chai');
var expect = chai.expect;
var superagent = require('superagent');
var request = require('request');
var moment = require('moment');
var async = require('async');
var fs = require('fs');
var WebSocket = require('ws');


var dhh = require('../DH_test_helpers/DHtesthelpers.js');
var testRandom = require('../js_testing_utils/randomUtils.js');
var WAIT_FOR_CHANNEL_RESPONSE_MS = 3000;

var URL_ROOT = dhh.URL_ROOT;

// Test variables that are regularly overwritten
var agent
    , payload
    , req
    , uri;

var channelName;


describe('Channel Subscription:', function() {

    before(function(myCallback){
        channelName = dhh.makeRandomChannelName();
        agent = superagent.agent();
        dhh.makeChannel(channelName, function(res){
            if ((res.error) || (res.status != 200)) {
                myCallback(res.error);
            };
            console.log('Main test channel:'+ channelName);
            myCallback();
        });
    });

    beforeEach(function(){
        agent = superagent.agent();
        payload = uri = req = ws = null;
    });

    it.skip('Acceptance: subscription works and updates are sent in order', function(done) {
        var ws = dhh.createWebSocket(channelName);
        var responseQueue = [];
        var pUri1, pUri2;

        ws.on('message', function(data, flags) {
            responseQueue.push(data);
        });

        // post values to channel
        async.parallel([
            function(callback){
                dhh.postData(channelName, testRandom.randomString(50), function(res, uri) {
                    expect(res.status).to.equal(dhh.DATA_POST_SUCCESS);
                    pUri1 = uri;
                    callback(null, null);
                });
            },
            function(callback){
                dhh.postData(channelName, testRandom.randomString(50), function(res, uri) {
                    expect(res.status).to.equal(dhh.DATA_POST_SUCCESS);
                    pUri2 = uri;
                    callback(null, null);
                });
            }
        ],
            function(e, r){});

        // Wait for some period of time for both values
        var endWait = Date.now() + (2 * WAIT_FOR_CHANNEL_RESPONSE_MS);

        while((responseQueue.length < 2) && (Date.now() < endWait)) {
            setTimeout(function () {
                // pass
            }, 100)
        };

        expect(responseQueue.length).to.equal(2);
        expect(responseQueue[0]).to.equal(pUri1);
        expect(responseQueue[1]).to.equal(pUri2);

        ws.close();

        done();
    });

    it.skip('Multiple nigh-simultaneous updates are sent with order preserved.', function(done) {
        this.timeout=60000;

        var ws = dhh.createWebSocket(channelName);
        var responseQueue = [];
        var pUri1, pUri2, pUri3, pUri4, pUri5;

        ws.on('message', function(data, flags) {
            responseQueue.push(data);
        });

        // post values to channel
        async.parallel([
            function(callback){
                dhh.postData(channelName, testRandom.randomString(50), function(res, uri) {
                    expect(res.status).to.equal(dhh.DATA_POST_SUCCESS);
                    pUri1 = uri;
                    callback(null, null);
                });
            },
            function(callback){
                dhh.postData(channelName, testRandom.randomString(50), function(res, uri) {
                    expect(res.status).to.equal(dhh.DATA_POST_SUCCESS);
                    pUri2 = uri;
                    callback(null, null);
                });
            },
            function(callback){
                dhh.postData(channelName, testRandom.randomString(50), function(res, uri) {
                    expect(res.status).to.equal(dhh.DATA_POST_SUCCESS);
                    pUri3 = uri;
                    callback(null, null);
                });
            },
            function(callback){
                dhh.postData(channelName, testRandom.randomString(50), function(res, uri) {
                    expect(res.status).to.equal(dhh.DATA_POST_SUCCESS);
                    pUri4 = uri;
                    callback(null, null);
                });
            },
            function(callback){
                dhh.postData(channelName, testRandom.randomString(50), function(res, uri) {
                    expect(res.status).to.equal(dhh.DATA_POST_SUCCESS);
                    pUri5 = uri;
                    callback(null, null);
                });
            }
        ],
            function(e, r){});

        // Wait for some period of time for both values
        var endWait = Date.now() + (5 * WAIT_FOR_CHANNEL_RESPONSE_MS);

        while((responseQueue.length < 5) && (Date.now() < endWait)) {
            setTimeout(function () {
                // pass
            }, 100)
        };

        expect(responseQueue.length).to.equal(5);
        expect(responseQueue[0]).to.equal(pUri1);
        expect(responseQueue[1]).to.equal(pUri2);
        expect(responseQueue[2]).to.equal(pUri3);
        expect(responseQueue[3]).to.equal(pUri4);
        expect(responseQueue[4]).to.equal(pUri5);

        ws.close();

        done();
    });

    it.skip('Multiple agents on a channel can be supported.', function(done) {
        done();
    });

    it.skip('Server recognizes when agent disconnects (in what time frame?)', function(done) {
        done();
    });

    it.skip('Disconnect and reconnect is supported.', function(done) {
        done();
    });

    it.skip('Multiple agents on multiple channels is handled appropriately.', function(done) {
        done();
    });
});
