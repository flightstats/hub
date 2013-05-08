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
var gu = require('../genericUtils.js');

var WAIT_FOR_CHANNEL_RESPONSE_MS = 10 * 1000;
var WAIT_FOR_SOCKET_CLOSURE_MS = 10 * 1000;

var URL_ROOT = dhh.URL_ROOT;
//var DOMAIN = '10.250.220.197:8080';
var DOMAIN = 'datahub-01.cloud-east.dev:8080';
var DEBUG = false;

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
            if ((res.error) || (!gu.isHTTPSuccess(res.status))) {
                myCallback(res.error);
            }
            myCallback();
        });
    });




    it('(alt) Acceptance: subscription works and updates are sent in order', function(done) {
        var socket,
            uri1, uri2; // remember, the numbers do NOT necessarily reflect the order of creation

        var mainTest = function() {
            // post values to channel
            async.parallel([
                function(callback){
                    dhh.postData(channelName, testRandom.randomString(50), function(res, uri) {
                        expect(gu.isHTTPSuccess(res.status)).to.equal(true);
                        dhh.debugLog('Posted first value ', DEBUG);
                        uri1 = uri;
                        callback(null, null);
                    });
                },
                function(callback){
                    dhh.postData(channelName, testRandom.randomString(50), function(res, uri) {
                        expect(gu.isHTTPSuccess(res.status)).to.equal(true);
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

                        while((socket.responseQueue.length < 2) && (Date.now() < endWait)) {
                            setTimeout(function () {
                                // pass
                            }, 100)
                        };

                        expect(socket.responseQueue.length).to.equal(2);
                        expect(socket.responseQueue[0]).to.equal(firstUri);
                        expect(socket.responseQueue[1]).to.equal(latestUri);

                        dhh.debugLog('final socket state is '+ socket.ws.readyState, DEBUG);

                        socket.ws.close();
                        done();
                    });
                });
        };

        socket =  new dhh.WSWrapper(DOMAIN, channelName, 'ws_01', mainTest);
        socket.createSocket();
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
                // pass
                /*
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
                */
            });
        };

        var confirmOrderOfResponses = function() {
            gu.debugLog('...entering confirmOrderOfResponses()');

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
        }

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

             if (actualResponseQueue.length == numUpdates) {
                 confirmOrderOfResponses();
             }
        });

        //process.nextTick();
    });

    it('Multiple agents on a channel can be supported.', function(done) {
        // Channel created
        // create twelve agents that subscribe to the channel
        // channel pumps out three bits of data
        // each channel receives data in correct order
        var sockets = [],
            numAgents = 12,
            numReadySockets = 0,
            uri1,   // remember, the numbers do NOT necessarily reflect the order of creation
            uri2;

        // Called from newSocketIsReady() if all sockets are ready
        var mainTest = function() {
            // post values to channel
            async.parallel([
                function(callback){
                    dhh.postData(channelName, testRandom.randomString(50), function(res, uri) {
                        expect(gu.isHTTPSuccess(res.status)).to.equal(true);
                        dhh.debugLog('Posted first value ', DEBUG);
                        uri1 = uri;
                        callback(null, null);
                    });
                },
                function(callback){
                    dhh.postData(channelName, testRandom.randomString(50), function(res, uri) {
                        expect(gu.isHTTPSuccess(res.status)).to.equal(true);
                        dhh.debugLog('Posted second value ', DEBUG);
                        uri2 = uri;
                        callback(null, null);
                    });
                }
            ],
                function(e, r){

                    // pass  (rewrote the stuff below and moved it out into testAllSockets() )
                    /*
                    dhh.debugLog('In final part of async ', DEBUG);
                    dhh.getLatestUriFromChannel(channelName, function(latestUri) {
                        var firstUri = (latestUri == uri1) ? uri2 : uri1;

                        //console.log('First uri: '+ firstUri);
                        //console.log('Second uri: '+ latestUri);

                        // Wait for some period of time for both values
                        var endWait = Date.now() + (2 * WAIT_FOR_CHANNEL_RESPONSE_MS);

                        while((numFullSockets() < numAgents) && (Date.now() < endWait)) {
                            setTimeout(function () {
                                // pass
                            }, 100)
                        };

                        expect(numFullSockets()).to.equal(numAgents);

                        for (var i = 0; i < sockets.length; i += 1) {
                            var thisSocket = sockets[i];

                            expect(thisSocket.responseQueue.length).to.equal(2);
                            expect(thisSocket.responseQueue[0]).to.equal(firstUri);
                            expect(thisSocket.responseQueue[1]).to.equal(latestUri);

                            dhh.debugLog('Final socket state for socket '+ thisSocket.name +' is '+ socket.ws.readyState, DEBUG);

                            thisSocket.ws.close();
                        }

                        done();
                    });
                    */
                });
        };

        // Called from afterOnMessage() if all sockets have received messages
        var testAllSockets = function() {
            dhh.debugLog('In final part of async ', DEBUG);
            dhh.getLatestUriFromChannel(channelName, function(latestUri) {
                var firstUri = (latestUri == uri1) ? uri2 : uri1;

                expect(numFullSockets()).to.equal(numAgents);

                for (var i = 0; i < sockets.length; i += 1) {
                    var thisSocket = sockets[i];

                    expect(thisSocket.responseQueue.length).to.equal(2);
                    expect(thisSocket.responseQueue[0]).to.equal(firstUri);
                    expect(thisSocket.responseQueue[1]).to.equal(latestUri);

                    dhh.debugLog('Final socket state for socket '+ thisSocket.name +' is '+ socket.ws.readyState, DEBUG);

                    thisSocket.ws.close();
                }

                done();
            });
        }

        // Called when each socket is ready.
        var newSocketIsReady = function() {
            numReadySockets += 1;
            if (numAgents === numReadySockets) {
                mainTest();
            }
        };

        // Returns the number of sockets that received the expected number of messages
        var numFullSockets = function() {
            var full = 0;

            for (var i = 0; i < sockets.length; i += 1) {
                if (2 === sockets[i].responseQueue.length) {
                    full += 1;
                }
            }

            return full;
        }

        // called when a socket's onMessage() is done
        var afterOnMessage = function() {
            if (numFullSockets() == numAgents) {
                testAllSockets();
            }
        }

        // Create yon sockets
        for (var i = 0; i < numAgents; i += 1) {
            //var socket =  new dhh.WSWrapper(DOMAIN, channelName, 'ws_'+ i, newSocketIsReady);
            var socket = new dhh.altWSWrapper({
                'domain': DOMAIN,
                'channel': channelName,
                'socketName': 'ws_'+ i,
                'onOpenCB': newSocketIsReady,
                'onMessageCB': afterOnMessage
            });

            socket.createSocket();
            sockets[i] = socket;
        }
    });

    it.skip('<NOT WRITTEN> Server recognizes when agent disconnects (in what time frame?)', function(done) {
        done();
    });

    it('Disconnecting and then adding a new agent on same channel works.', function(done) {
        var socket1, socket2, uri1, uri2;

        var socket1OnOpen = function() {
            // broadcast message; confirm socket 1 received
            gu.debugLog('...entering socket1 Open function', DEBUG);

            dhh.postData(channelName, testRandom.randomString(50), function(res, uri) {
                expect(gu.isHTTPSuccess(res.status)).to.equal(true);
                dhh.debugLog('Posted first value ', DEBUG);
                uri1 = uri;
            });

            /*

            dhh.debugLog('Starting Socket1 main section', DEBUG);

            async.series([
                function(callback){
                    dhh.postData(channelName, testRandom.randomString(50), function(res, uri) {
                        expect(gu.isHTTPSuccess(res.status)).to.equal(true);
                        dhh.debugLog('Posted first value ', DEBUG);
                        uri1 = uri;
                        callback(null, null);
                    });
                },
                function(callback){
                    var endWait = Date.now() + WAIT_FOR_CHANNEL_RESPONSE_MS;

                    while((0 === socket1.responseQueue.length) && (Date.now() < endWait)) {
                        setTimeout(function () {
                            // pass
                        }, 1000)
                    };

                    expect(socket1.responseQueue.length).to.equal(1);
                    expect(uri1).to.equal(socket1.responseQueue[0]);
                    dhh.debugLog('Confirmed first socket received msg', DEBUG);

                    expect(socket2.responseQueue.length).to.equal(0);   // socket2 not connected yet
                    dhh.debugLog('Confirmed second socket queue is empty', DEBUG);

                    callback(null, null);
                },
                function(callback){
                    dhh.debugLog('Calling socket1.close()', DEBUG);
                    socket1.ws.close();
                    dhh.debugLog('About to create second socket', DEBUG);
                    socket2.createSocket();

                    socket2.ws.on('close', function(code, message) {
                        dhh.debugLog('Socket2 closed', DEBUG);

                        done();
                    });

                    callback(null,null);
                }
            ]);
            */
        };

        var socket2OnOpen = function() {
            dhh.debugLog('...entering socket2 Open function', DEBUG);

            expect(socket2.responseQueue.length).to.equal(0);

            dhh.postData(channelName, testRandom.randomString(50), function(res, uri) {
                expect(gu.isHTTPSuccess(res.status)).to.equal(true);
                dhh.debugLog('Posted second value ', DEBUG);
                uri2 = uri;
            });

            /*
             async.series([
             function(callback){
             expect(socket2.responseQueue.length).to.equal(0);

             dhh.postData(channelName, testRandom.randomString(50), function(res, uri) {
             expect(gu.isHTTPSuccess(res.status)).to.equal(true);
             dhh.debugLog('Posted second value ', DEBUG);
             uri2 = uri;
             callback(null, null);
             });
             },
             function(callback){
             var endWait = Date.now() + WAIT_FOR_CHANNEL_RESPONSE_MS;

             dhh.debugLog('Starting wait for second socket to receive msg', DEBUG);

             while((0 === socket2.responseQueue.length) && (Date.now() < endWait)) {
             setTimeout(function () {
             // pass
             }, 1000)
             };

             expect(socket2.responseQueue.length).to.equal(1);
             expect(uri2).to.equal(socket2.responseQueue[0]);
             dhh.debugLog('Confirmed second socket received msg', DEBUG);

             callback(null, null);
             },
             function(callback){
             socket2.ws.close();

             callback(null,null);
             }
             ]);
             */
        };

        // Called at end of onMessage event for socket 1
        var socket1Msg = function() {
            gu.debugLog('...entering socket1 Message');

            if (undefined == uri1) {
                setTimeout(function() {
                    finishSocket1();
                }, 3000)
            }
            else {
                finishSocket1()
            }
        }

        // Called at the end of the onMessage event for socket 2
        var socket2Msg = function() {
            gu.debugLog('...entering socket2Msg()');

            if (undefined == uri2) {
                setTimeout(function() {
                    finishSocket2();
                }, 3000)
            }
            else {
                finishSocket2();
            }
        }

        var finishSocket1 = function() {
            gu.debugLog('... entering finishSocket1');

            expect(socket1.responseQueue.length).to.equal(1);
            expect(uri1).to.equal(socket1.responseQueue[0]);
            dhh.debugLog('Confirmed first socket received msg', DEBUG);

            expect(socket2.responseQueue.length).to.equal(0);   // socket2 not connected yet
            dhh.debugLog('Confirmed second socket queue is empty', DEBUG);

            dhh.debugLog('Calling socket1.close()', DEBUG);
            socket1.ws.close();
            dhh.debugLog('About to create second socket', DEBUG);
            socket2.createSocket();

            socket2.ws.on('close', function(code, message) {
                dhh.debugLog('Socket2 closed', DEBUG);
                done();
            });

        }

        var finishSocket2 = function() {
            gu.debugLog('...entering finishSocket2()');

            expect(socket2.responseQueue.length).to.equal(1);
            expect(uri2).to.equal(socket2.responseQueue[0]);
            dhh.debugLog('Confirmed second socket received msg', DEBUG);

            socket2.ws.close();
        }

        //socket1 = new dhh.WSWrapper(DOMAIN, channelName, 'ws_1', socket1Main);
        //socket2 = new dhh.WSWrapper(DOMAIN, channelName, 'ws_2', socket2Main);
        socket1 = new dhh.altWSWrapper({
            'domain': DOMAIN,
            'channel': channelName,
            'socketName': 'ws_1',
            'onOpenCB': socket1OnOpen,
            'onMessageCB': socket1Msg
        });

        socket2 = new dhh.altWSWrapper({
            'domain': DOMAIN,
            'channel': channelName,
            'socketName': 'ws_2',
            'onOpenCB': socket2OnOpen,
            'onMessageCB': socket2Msg
        });

        socket1.createSocket();

        socket1.ws.on('close', function(code, message) {
            dhh.debugLog('Socket1 closed', DEBUG);
        });
    });

    it.skip('<NOT WRITTEN> Multiple agents on multiple channels is handled appropriately.', function(done) {
        done();
    });

    // ***********  DEPRECATED TESTS **************** //
    /*
    it('Acceptance: subscription works and updates are sent in order', function(done) {
        var responseQueue = [], ws;
        var uri1, uri2; // remember, the numbers do NOT necessarily reflect the order of creation

        var mainTest = function() {
            // post values to channel
            async.parallel([
                function(callback){
                    dhh.postData(channelName, testRandom.randomString(50), function(res, uri) {
                        expect(gu.isHTTPSuccess(res.status)).to.equal(true);
                        dhh.debugLog('Posted first value ', DEBUG);
                        uri1 = uri;
                        callback(null, null);
                    });
                },
                function(callback){
                    dhh.postData(channelName, testRandom.randomString(50), function(res, uri) {
                        expect(gu.isHTTPSuccess(res.status)).to.equal(true);
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
    */

});
