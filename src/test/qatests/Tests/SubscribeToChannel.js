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
var WAIT_FOR_CHANNEL_RESPONSE_MS = 500;
var WAIT_FOR_SOCKET_OPEN_MS = 3000;

var URL_ROOT = dhh.URL_ROOT;
//var DOMAIN = '10.250.220.197:8080';
var DOMAIN = 'datahub-01.cloud-east.dev:8080';

// Test variables that are regularly overwritten
var agent
    , payload
    , req
    , uri;

var channelName;


describe('Channel Subscription:', function() {

    before(function(){
        console.log('\nURL_ROOT: '+ URL_ROOT);
        console.log('DOMAIN (for websockets): '+ DOMAIN);
    });

    beforeEach(function(myCallback){
        agent = superagent.agent();
        payload = uri = req = null;

        channelName = 'gtest';

        /*
        channelName = dhh.makeRandomChannelName();
        dhh.makeChannel(channelName, function(res){
            if ((res.error) || (res.status != 200)) {
                myCallback(res.error);
            };
            myCallback();
        });
        */
        myCallback();
    });

    it('Acceptance: subscription works and updates are sent in order', function(done) {
        var ws = dhh.createWebSocket(DOMAIN, channelName);
        var responseQueue = [];
        var uri1, uri2; // remember, the numbers do NOT necessarily reflect the order of creation

        ws.on('message', function(data, flags) {
            responseQueue.push(data);
        });

        // post values to channel
        async.parallel([
            function(callback){
                dhh.postData(channelName, testRandom.randomString(50), function(res, uri) {
                    expect(res.status).to.equal(dhh.DATA_POST_SUCCESS);
                    uri1 = uri;
                    callback(null, null);
                });
            },
            function(callback){
                dhh.postData(channelName, testRandom.randomString(50), function(res, uri) {
                    expect(res.status).to.equal(dhh.DATA_POST_SUCCESS);
                    uri2 = uri;
                    callback(null, null);
                });
            }
        ],
            function(e, r){
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

                    ws.close();
                    done();
                });
            });
    });

    it.skip('Multiple nigh-simultaneous updates are sent with order preserved.', function(done) {
        var ws = dhh.createWebSocket(DOMAIN, channelName);
        var actualResponseQueue = [], expectedResponseQueue = [], endWait;
        var numUpdates = 10;

        this.timeout((numUpdates * WAIT_FOR_CHANNEL_RESPONSE_MS) + 45000);

        ws.on('message', function(data, flags) {
            actualResponseQueue.push(data);
            console.log('Received message: '+ data);
        });

        endWait = Date.now() + WAIT_FOR_SOCKET_OPEN_MS;

        while((WebSocket.OPEN != ws.readyState) && (Date.now() < endWait)) {
            setTimeout(function() {
                // just chillin'
            }, 100)
        };

        async.times(numUpdates, function(n, next) {
            dhh.postDataAndWait(channelName, testRandom.randomString(50), 1000, function(res, uri) {
                console.log('Posted data #'+ n);
                next(null, uri);
            });
        }, function(err, uris) {
            //console.log('Entering post-async function');
            console.log('Number of entries in actual response queue: '+ actualResponseQueue.length);

            // Repeatedly wait and check response queue from socket until we give up or reach expected number
            endWait = Date.now() + (numUpdates * WAIT_FOR_CHANNEL_RESPONSE_MS);
            var lastQueueLength = actualResponseQueue.length;   // for debugging

            while((actualResponseQueue.length < numUpdates) && (Date.now() < endWait)) {
                setTimeout(function () {
                    if (actualResponseQueue > lastQueueLength) {
                        console.log('Response Queue at '+ actualResponseQueue.length);
                        lastQueueLength = actualResponseQueue.length;
                    }
                }, 100)
            };

            if (actualResponseQueue.length != numUpdates) {
                console.log('Timed out before response queue got all updates.');
                expect(actualResponseQueue.length).to.equal(numUpdates);
            }

            // confirm results
            dhh.getListOfLatestUrisFromChannel(numUpdates, channelName, function(allUris) {
                expectedResponseQueue = allUris;

                expect(actualResponseQueue.length).to.equal(numUpdates);
                expect(expectedResponseQueue.length).to.equal(numUpdates);

                console.log('Expected and Actual queues are full. Comparing queues...');

                for (var i = 0; i < numUpdates; i += 1) {
                    expect(actualResponseQueue[i]).to.equal(expectedResponseQueue[i]);
                    console.log('Matched queue number '+ i);
                }

                ws.close();
                done();
            });
        });
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
