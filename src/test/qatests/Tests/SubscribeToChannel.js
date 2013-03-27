"use strict";

var chai = require('chai');
var expect = chai.expect;
var superagent = require('superagent');
var request = require('request');
var moment = require('moment');
var async = require('async');
var fs = require('fs');
var WebSocket = require('ws');


var dhh = require('../DH_test_helpers/DHtesthelpers.js');
var testRandom = require('../randomUtils.js');
var WAIT_FOR_CHANNEL_RESPONSE_MS = 3000;

var URL_ROOT = dhh.URL_ROOT;
//var DOMAIN = '10.250.220.197:8080';
var DOMAIN = 'datahub-01.cloud-east.dev:8080';
var DEBUG = true;

// Test variables that are regularly overwritten
var agent, payload, req, uri;

var channelName;


describe('Channel Subscription:', function() {

    before(function(){
        dhh.debugLog('\nURL_ROOT: '+ URL_ROOT);
        dhh.debugLog('DOMAIN (for websockets): '+ DOMAIN);
        dhh.debugLog('Debugging ENABLED', DEBUG);
    });

    beforeEach(function(myCallback){
        agent = superagent.agent();
        payload = uri = req = null;

        channelName = dhh.makeRandomChannelName();

        dhh.makeChannel(channelName, function(res){
            if ((res.error) || (!dhh.isHTTPSuccess(res.status))) {
                myCallback(res.error);
            }
            myCallback();
        });
    });

    it('Acceptance: subscription works and updates are sent in order', function(done) {
        var responseQueue = [], ws;
        var uri1, uri2; // remember, the numbers do NOT necessarily reflect the order of creation

        var mainTest = function() {
            // post values to channel
            async.parallel([
                function(callback){
                    dhh.postData(channelName, testRandom.randomString(50), function(res, uri) {
                        expect(dhh.isHTTPSuccess(res.status)).to.equal(true);
                        dhh.debugLog('Posted first value ', DEBUG);
                        uri1 = uri;
                        callback(null, null);
                    });
                },
                function(callback){
                    dhh.postData(channelName, testRandom.randomString(50), function(res, uri) {
                        expect(dhh.isHTTPSuccess(res.status)).to.equal(true);
                        dhh.debugLog('Posted second value ', DEBUG);
                        uri2 = uri;
                        callback(null, null);
                    });
                }
            ],
                function(e, r){
                    dhh.debugLog('In final part of async ', DEBUG);
                    dhh.getLatestUriFromChannel(channelName, function(latestUri) {
                        var firstUri = (latestUri == uri1) ? uri2 : uri1;

                        //console.log('First uri: '+ firstUri);
                        //console.log('Second uri: '+ latestUri);

                        // Wait for some period of time for both values
                        var endWait = Date.now() + (2 * WAIT_FOR_CHANNEL_RESPONSE_MS);

                        while((responseQueue.length < 2) && (Date.now() < endWait)) {
                            setTimeout(function () {
                                // pass
                            }, 100)
                        };

                        expect(responseQueue.length).to.equal(2);
                        expect(responseQueue[0]).to.equal(firstUri);
                        expect(responseQueue[1]).to.equal(latestUri);

                        console.log('final socket state is '+ ws.readyState);

                        ws.close();
                        done();
                    });
                });
        };

        var onOpen = function() {
            console.log('OPEN EVENT at '+ Date.now());
            console.log('readystate: '+ ws.readyState)   ;
            mainTest();
        };

        ws = dhh.createWebSocket(DOMAIN, channelName, onOpen);

        ws.on('message', function(data, flags) {
            console.log('MESSAGE EVENT at '+ Date.now());
            dhh.debugLog('Readystate is '+ ws.readyState, DEBUG);
            responseQueue.push(data);
        });
    });

    // Note, this test also ensures that all updates are correctly saved in the DH *and* that their
    //  relative links are correct.
    it('Multiple nigh-simultaneous updates are sent with order preserved.', function(done) {
        var actualResponseQueue = [], expectedResponseQueue = [], endWait, i;
        var numUpdates = 10, doDebug = false;
        this.timeout((numUpdates * WAIT_FOR_CHANNEL_RESPONSE_MS) + 45000);

        var mainTest = function() {
            async.times(numUpdates, function(n, next) {
                dhh.postData(channelName, testRandom.randomString(50), function(res, uri) {
                    dhh.debugLog('Posted data #'+ n, DEBUG);
                    next(null, uri);
                });
            }, function(err, uris) {
                dhh.debugLog('Number of entries in actual response queue: '+ actualResponseQueue.length, DEBUG);

                // Repeatedly wait and check response queue from socket until we give up or reach expected number
                endWait = Date.now() + (numUpdates * WAIT_FOR_CHANNEL_RESPONSE_MS);
                var lastQueueLength = actualResponseQueue.length;   // for debugging

                var checkQueueLength = function () {
                    if (actualResponseQueue > lastQueueLength) {
                        dhh.debugLog('Response Queue at '+ actualResponseQueue.length, DEBUG);
                        lastQueueLength = actualResponseQueue.length;
                    }
                };

                while((actualResponseQueue.length < numUpdates) && (Date.now() < endWait)) {
                    setTimeout( checkQueueLength(), 100);
                }
                dhh.debugLog('Passed while loop with actual queue at '+ actualResponseQueue.length, DEBUG);

                if (actualResponseQueue.length !== numUpdates) {
                    dhh.debugLog('Timed out before response queue got all updates.', DEBUG);
                    expect(actualResponseQueue.length).to.equal(numUpdates);
                }

                // confirm results
                dhh.getListOfLatestUrisFromChannel(numUpdates, channelName, function(allUris) {
                    expectedResponseQueue = allUris;
                    dhh.debugLog('Expected response queue length: '+ expectedResponseQueue.length, DEBUG);

                    expect(actualResponseQueue.length).to.equal(numUpdates);
                    expect(expectedResponseQueue.length).to.equal(numUpdates);

                    dhh.debugLog('Expected and Actual queues are full. Comparing queues...', DEBUG);

                    for (i = 0; i < numUpdates; i += 1) {
                        expect(actualResponseQueue[i]).to.equal(expectedResponseQueue[i]);
                        dhh.debugLog('Matched queue number '+ i, DEBUG);
                    }

                    ws.close();
                    done();
                });
            });
        };

        var onOpen = function() {
            dhh.debugLog('Open event fired!', DEBUG);
            dhh.debugLog('Readystate: '+ ws.readyState, DEBUG);
            mainTest();
        };

        var ws = dhh.createWebSocket(DOMAIN, channelName, onOpen);

        ws.on('message', function(data, flags) {
            actualResponseQueue.push(data);
            dhh.debugLog('Received message: '+ data, DEBUG);
            dhh.debugLog('Response queue length: '+ actualResponseQueue.length, DEBUG);
        });

        //process.nextTick();
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
