"use strict";

var chai = require('chai');
var expect = chai.expect;
var superagent = require('superagent'),
    request = require('request'),
    moment = require('moment'),
    async = require('async'),
    fs = require('fs'),
    WebSocket = require('ws'),
    lodash = require('lodash'),
    url = require('url');


var dhh = require('../DH_test_helpers/DHtesthelpers.js'),
    ranU = require('../randomUtils.js'),
    gu = require('../genericUtils.js');

var WAIT_FOR_CHANNEL_RESPONSE_MS = 10 * 1000,
    WAIT_FOR_SOCKET_CLOSURE_MS = 10 * 1000,
    URL_ROOT = dhh.URL_ROOT,
    MY_8MB_FILE = './artifacts/Iam8Mb.txt',
    DOMAIN = dhh.DOMAIN,
//    DOMAIN = 'hub-02.cloud-east.staging:8080',
    FAKE_SOCKET_URI = ['ws:/', dhh.DOMAIN, 'channel', 'sQODTvsYlLOLWTFPWNBBQ', 'ws'].join('/'),
    LOAD_BALANCER_HOSTNAME = dhh.DOMAIN,
    DEBUG = true;




describe('Channel Subscription:', function() {

    // Test variables that are regularly overwritten
    var payload, req, uri;

    var channelName,
        postHostCnUri,  // used for tests that need to hit specific hosts
        channelUri,
        postHostWsUri,
        wsUri;

    var makeCnUri = function(host) {
        return ['http:/', host, postHostCnUri].join('/');
    }

    var makeWsUri = function(host) {
        return ['ws:/', host, postHostWsUri].join('/');
    }

    var getPostHostUri = function(uri) {
        var hostRegex = /\/\/([^/]+)\/(.+)/,
            m = hostRegex.exec(uri),
            theRest = m[2];

        return theRest;
    }

    var doUrlsMatch = function(params) {
        var urlA = url.parse(params.urlA),
            urlB = url.parse(params.urlB),
            doIgnorePort = (undefined != params.doIgnorePort) ? params.doIgnorePort : true,
            doIgnoreHost = (undefined != params.doIgnoreHost) ? params.doIgnoreHost : false;

        // one-off function to deliver text version of url in known format
        var spitIt = function(obj) {
            var port = (obj.port) ? ':'+ obj.port : '',
                text = obj.protocol +'//'+ obj.hostname + port + obj.path;

            return text;
        }

        if (doIgnorePort) {
            urlA['port'] = urlB['port'];
        }

        if (doIgnoreHost) {
            urlA['hostname'] = urlB['hostname'];
        }

        return (spitIt(urlA) == spitIt(urlB))
    }

    before(function(){
        gu.debugLog('\nURL_ROOT: '+ URL_ROOT);
        gu.debugLog('DOMAIN (for websockets): '+ DOMAIN);
        gu.debugLog('Debugging ENABLED', DEBUG);
    });

    beforeEach(function(myCallback){
        payload = uri = req = null;

        /*
        // temporarily changed to hardcoded channel
        dhh.getChannel({name: 'lolcats'}, function(res, body) {
            if ((res.error) || (!gu.isHTTPSuccess(res.status))) {
                myCallback(res.error);
            }
            var cnMetadata = new dhh.channelMetadata(body);
            channelUri = cnMetadata.getChannelUri();
            wsUri = cnMetadata.getWebSocketUri();

            // Need to test this!
            var hostRegex = /\/\/[^\/]+\//;
            var m = hostRegex.exec(channelUri);

            postHostCnUri = m[1];
            m = hostRegex.exec(wsUri);
            postHostWsUri = m[1];

            myCallback();
        })
        */


        channelName = dhh.getRandomChannelName();     
//        channelName = 'gabe_'+ dhh.getRandomChannelName();


        dhh.createChannel({name: channelName, domain: DOMAIN }, function(res){
            if ((res.error) || (!gu.isHTTPSuccess(res.status))) {
                myCallback(res.error);
            }
            var cnMetadata = new dhh.channelMetadata(res.body);
            channelUri = cnMetadata.getChannelUri();
            wsUri = cnMetadata.getWebSocketUri();

            gu.debugLog('cnProxyUri: '+ channelUri);
            gu.debugLog('wsUri: '+ wsUri);

            postHostCnUri = getPostHostUri(channelUri);
            postHostWsUri = getPostHostUri(wsUri);

            myCallback();
        });

    });

    it('Acceptance: subscription works and updates are sent in order', function(done) {
        var socket,
            uriA,
            uriB;

        var afterOpen = function() {
            dhh.postData({channelUri: channelUri, data: dhh.getRandomPayload()}, function(res, uri) {
                expect(gu.isHTTPSuccess(res.status)).to.equal(true);
                gu.debugLog('Posted priming value ', DEBUG);
            })
        };

        var postMainItems = function() {
            async.parallel([
                function(callback){
                    dhh.postData({channelUri: channelUri, data: dhh.getRandomPayload()}, function(res, uri) {
                        expect(gu.isHTTPSuccess(res.status)).to.equal(true);
                        gu.debugLog('Posted first value ', DEBUG);
                        uriA = uri;
                        callback(null, null);
                    });
                },
                function(callback){
                    dhh.postData({channelUri: channelUri, data: dhh.getRandomPayload()}, function(res, uri) {
                        expect(gu.isHTTPSuccess(res.status)).to.equal(true);
                        gu.debugLog('Posted second value ', DEBUG);
                        uriB = uri;
                        callback(null, null);
                    });
                }
            ])
        };


        var afterMessage = function() {
            if (1 == socket.responseQueue.length) {
                postMainItems();
            }
            else {
                if (DEBUG) {
                    gu.debugLog('MESSAGE RECEIVED: ' + socket.responseQueue[socket.responseQueue.length - 1]);
                }

                if (socket.responseQueue.length == 3)  {
                    confirmSocketData();
                }
            }
        };

        var confirmSocketData = function() {
            dhh.getLatestUri(channelUri, function(latestUri) {
                var firstUri = (latestUri == uriA) ? uriB : uriA;

                // Updated length to include priming message
                expect(socket.responseQueue.length).to.equal(3);

                //expect(socket.responseQueue[0]).to.equal(firstUri);
                //expect(socket.responseQueue[1]).to.equal(latestUri);

                expect(doUrlsMatch({urlA: socket.responseQueue[1], urlB: firstUri})).to.be.true;
                expect(doUrlsMatch({urlA: socket.responseQueue[2], urlB: latestUri})).to.be.true;

                gu.debugLog('final socket state is '+ socket.ws.readyState, DEBUG);

                socket.ws.close();
                done();
            });
        };

        socket = new dhh.WSWrapper({
            'domain': DOMAIN,
            'uri': wsUri,
            'socketName': 'ws_01',
            'onOpenCB': afterOpen,
            'onMessageCB': afterMessage
        });
        socket.createSocket();

    });

    it('Returns 404 (as error) attempting to connect to a fake channel ws URI', function(done) {
        var socket;

        // Error is triggered and the message contains '404'
        var onError = function(msg) {
            expect(msg.toString().indexOf('404')).to.be.at.least(0);

            done();
        }

        socket = new dhh.WSWrapper({
            'domain': DOMAIN,
            'uri': FAKE_SOCKET_URI,
            'socketName': 'takeMeInDryTheRain',
            'onOpenCB': null,
            'onErrorCB': onError
        });

        socket.createSocket();
    })

    // Attach a listener to each instance and to the load balancer, all on the same channel.
    // Insert items in parallel into that channel, directly into each instance and into the load balancer.
    // Ensure that the messages are reported in order.
    // https://www.pivotaltracker.com/story/show/52726289
    it.skip('HA: multiple parallel updates with a socket ' +
        'on each DH instance and the load balancer are reported in order', function(done) {

        // Configurable items
        var actualResponsesObjects = [
                {'host':'hub-01.cloud-east.dev:8080', 'cnUri': null, 'wsUri': null, socket: null},
                {'host':'hub-02.cloud-east.dev:8080', 'cnUri': null, 'wsUri': null, socket: null},
                {'host':'hub-03.cloud-east.dev:8080', 'cnUri': null, 'wsUri': null, socket: null},
//                {'host':'hub-01.cloud-east.staging:8080', 'cnUri': null, 'wsUri': null, socket: null},
//                {'host':'hub-02.cloud-east.staging:8080', 'cnUri': null, 'wsUri': null, socket: null},
//                {'host':'hub-03.cloud-east.staging:8080', 'cnUri': null, 'wsUri': null, socket: null},
                {'host':LOAD_BALANCER_HOSTNAME, 'cnUri': null, 'wsUri': null, socket: null}
            ],
            lbchannelUri = makeCnUri(LOAD_BALANCER_HOSTNAME),
            numItemsPerHostToPost = 100,

        // if true, then item inserts will be spread across hosts; if false, then all inserts go through load balancer.
        // Ideally, this is set to true (exploits more possible cases).
            postDirectlyThroughHosts = true,
            VERBOSE = false;

        // Global variables
        var numHosts = actualResponsesObjects.length,
            totalItemsToPost = numItemsPerHostToPost * numHosts,
            numOpenedSockets = 0,
            expectedResponseQueue = [];

        if (VERBOSE) {
            gu.debugLog('postHostCnUri: '+ postHostCnUri);
            gu.debugLog('postHostWsUri: '+ postHostWsUri);
        }

        // Set timeout
        var maxTimeout = (totalItemsToPost * WAIT_FOR_CHANNEL_RESPONSE_MS) + 45000,
            minTimeout = 180000,
            actTimeout = (maxTimeout <= minTimeout) ? maxTimeout : minTimeout;

        this.timeout(actTimeout);

        var dumpAllQueues = function() {
            gu.debugLog('******************************************\nDumping each host\'s queue.');

            lodash.forEach(actualResponsesObjects, function(obj) {
                gu.debugLog('Host uri'+ obj.cnUri +': ');
                console.dir(obj.socket.responseQueue);
            })
        }

        // Post all items for all hosts, calling confirmResults() afterwards if no errors.
        var doAllPosting = function() {

            // Called by async.map for each host to allow parallel posting by hosts
            var postForHost = function(hostObj, callback) {

                // called by async.times below to do each post
                var post = function(index, postCB) {

                    // post either through each host or all through LB
                    var cnUri = (postDirectlyThroughHosts) ? hostObj.cnUri : lbchannelUri;

                    dhh.postData({channelUri: cnUri, data: dhh.getRandomPayload(), debug: VERBOSE}, function(postRes, dataUri) {
                        var err = (gu.isHTTPSuccess(postRes.status)) ? null : postRes.status;
                        gu.debugLog('New data at '+ dataUri, ((null == err) && (VERBOSE)));
                        gu.debugLog('Error posting to '+ hostObj.cnUri +': '+ postRes.status, (null != err));

                        postCB(err, dataUri);
                    })
                };

                // Call post() n times in parallel (n = numItemsPerHostToPost)
                async.times(numItemsPerHostToPost, function(n, next) {
                    post(n, function(err, dataUri) {
                        next(err, dataUri);
                    })
                },
                function(err, dataUris) {
                    gu.debugLog('Error response: '+ err, (null != err));

                    callback(err, null);
                })
            }

            // Parallel version of for-each-host...
            async.map(actualResponsesObjects, postForHost, function(err, results) {
                gu.debugLog('At least one error occurred during posting: '+ err, (null != err));
                expect(err).to.be.null;
            })
        };

        // Confirm all queues match and are correct, then call endTest()
        var confirmResults = function() {

            var lastHost = null,
                thisHost = null,
                lastQueue = [],
                thisQueue = [];

            // Confirm each host's queue is identical.
            for (var i = 0; i < actualResponsesObjects.length; i += 1) {
                if (null != thisHost) {
                    lastHost = thisHost;
                    lastQueue = thisQueue;
                }
                thisHost = actualResponsesObjects[i];
                thisQueue = thisHost.socket.responseQueue;

                if (null == lastHost) {
                    continue;
                }

                // Confirm queue lengths match before bothering to check each item
                if (thisQueue.length != lastQueue.length) {
                    gu.debugLog('Different number of items in socket queues!');
                    gu.debugLog(lastHost.host +' has '+ lastQueue.length +' items.');
                    gu.debugLog(thisHost.host +' has '+ thisQueue.length +' items.');

                    dumpAllQueues();
                    expect(thisQueue.length != lastQueue.length);
                }

                // Check each item is a match
                for (var j = 0; j < lastQueue.length; j += 1) {
                    var lastItem = lastQueue[j],
                        thisItem = thisQueue[j];

                    //if (thisItem != lastItem)
                    if (!doUrlsMatch({urlA: thisItem, urlB: lastItem, doIgnoreHost: true}))
                    {
                        gu.debugLog('Mismatch in socket queues!');
                        gu.debugLog(lastHost.host +' has url '+ lastItem +' at index '+ j);
                        gu.debugLog(thisHost.host +' has url '+ thisItem +' at index '+ j);

                        dumpAllQueues();
                        //expect(thisItem).to.equal(lastItem);
                        expect(doUrlsMatch({urlA: thisItem, urlB: lastItem, doIgnoreHost: true})).to.be.true;
                    }
                }

                gu.debugLog('Successfully matched queues for '+ lastHost.host +' and '+ thisHost.host);
            }

            // Get correct list from backend
            dhh.getListOfLatestUrisFromChannel({numItems: numItemsPerHostToPost * numHosts, channelUri: lbchannelUri},
            function(allUris) {
                expectedResponseQueue = allUris;
                var actualResponseQueue = actualResponsesObjects[0]['socket']['responseQueue'];

                gu.debugLog('Total items posted: '+ totalItemsToPost);
                gu.debugLog('Expected response queue length: '+ expectedResponseQueue.length);
                gu.debugLog('Actual response queue length: '+ actualResponseQueue.length);

                if ((actualResponseQueue.length != totalItemsToPost) || (expectedResponseQueue.length != totalItemsToPost)) {
                    dumpAllQueues();
                }

                expect(actualResponseQueue.length).to.equal(totalItemsToPost);
                expect(expectedResponseQueue.length).to.equal(totalItemsToPost);

                gu.debugLog('Expected and Actual queues are full. Comparing queues...', VERBOSE);

                for (var i = 0; i < totalItemsToPost; i += 1) {
                    var actTrimmed = getPostHostUri(actualResponseQueue[i]),
                        expTrimmed = getPostHostUri(expectedResponseQueue[i]);

                    //expect(actTrimmed).to.equal(expTrimmed);
                    expect(doUrlsMatch({urlA: actTrimmed, urlB: expTrimmed, doIgnoreHost: true})).to.be.true;
                    gu.debugLog('Matched queue number '+ i, VERBOSE);
                }

                endTest();
            })
        }

        // Closes sockets at end of test.
        var endTest = function() {
            gu.debugLog('Ending test.', VERBOSE);

            lodash.forEach(actualResponsesObjects, function(hostObj) {
                hostObj.socket.ws.close();
            })

            if (true) {
                dumpAllQueues();
            }

            done();
        }

        // Increment numOpenedSockets. If all sockets are open, call doAllPosting()
        var onOpen = function() {
            numOpenedSockets += 1;

            if (numOpenedSockets >= numHosts) {
                doAllPosting();
            }
        };

        // If all sockets are full, call confirmResults()
        var onMsg = function() {
            var numFullSockets = 0;

            lodash.forEach(actualResponsesObjects, function(obj, key) {
                if (obj.socket.responseQueue.length >= totalItemsToPost) {
                    numFullSockets += 1;
                }
            })
            gu.debugLog('Number of full sockets: '+ numFullSockets, VERBOSE);

            if (numFullSockets >= numHosts) {
                confirmResults();
            }
        }

        // Flesh out / instantiate host objects with their child sockets and more info.
        lodash.forEach(actualResponsesObjects, function(obj, key) {
            gu.debugLog('Instantiating socket for '+ obj.host, VERBOSE);
            obj.cnUri = makeCnUri(obj.host);
            obj.wsUri = makeWsUri(obj.host);

            obj.socket = new dhh.WSWrapper({
                domain: obj.host,
                uri: obj.wsUri,
                socketName: obj.host,
                onOpenCB: onOpen,
                onMessageCB: onMsg,
                debug: false
            });

            obj.socket.createSocket();

            obj.socket.ws.on('close', function(code, message) {
                gu.debugLog('Socket closed ('+ obj.host +')', DEBUG);
            })
        });
    });

    // NOTE: in the new Hub, we only guarantee sequence preservation at 5 calls / second max, so this test cannot
    //  post more than 5 items. There is a known issue where a new websocket needs to be primed or we can miss the
    //  first one or two items if a bunch are sent in at once.
    it('Multiple nigh-simultaneous updates are sent with order preserved.', function(done) {
        var actualResponseQueue = [], expectedResponseQueue = [], endWait, i;
        var numUpdates = 6,
            VERBOSE = true;
        this.timeout((numUpdates * WAIT_FOR_CHANNEL_RESPONSE_MS) + 45000);

        var mainTest = function() {
            async.times(numUpdates - 1, function(n, next) {
                dhh.postData({channelUri: channelUri, data: dhh.getRandomPayload()}, function(res, uri) {
                    gu.debugLog('Posted data #'+ n, DEBUG);
                    next(null, uri);
                });
            }, function(err, uris) {
                // pass
            });
        };


        // Confirms order of responses and then calls confirmAllRelativeLinks(), which ends test
        var confirmOrderOfResponses = function() {
            gu.debugLog('...entering confirmOrderOfResponses()');

            dhh.getListOfLatestUrisFromChannel({numItems: numUpdates, channelUri: channelUri}, function(allUris) {
                expectedResponseQueue = allUris;
                gu.debugLog('Expected response queue length: '+ expectedResponseQueue.length, DEBUG);

                expect(actualResponseQueue.length).to.equal(numUpdates);
                expect(expectedResponseQueue.length).to.equal(numUpdates);

                gu.debugLog('Expected and Actual queues are full. Comparing queues...', DEBUG);

                for (i = 0; i < numUpdates; i += 1) {
                    //expect(actualResponseQueue[i]).to.equal(expectedResponseQueue[i]);
                    gu.debugLog('Attempting to match queue number '+ i, DEBUG)
                    expect(doUrlsMatch({urlA: actualResponseQueue[i], urlB: expectedResponseQueue[i]})).to.be.true;
//                    gu.debugLog('Matched queue number '+ i, DEBUG);
                }

                confirmAllRelativeLinks();
            });
        }

        // Test next/prev for each uri, as well as latest for channel. Then end test.
        var confirmAllRelativeLinks = function() {
            gu.debugLog('...entering confirmAllRelativeLinks()');

            dhh.testRelativeLinkInformation({channelUri: channelUri, numItems: numUpdates, debug: VERBOSE}, function(err) {
                if (null != err) {

                    gu.debugLog('Error in relative links test: '+ err);
                    expect(err).to.be.null;
                }

                ws.close();
                done();
            })
        }

        var onOpen = function() {
            gu.debugLog('Open event fired!', DEBUG);
            gu.debugLog('Readystate: '+ ws.readyState, DEBUG);

            dhh.postData({channelUri: channelUri, data: dhh.getRandomPayload()}, function() {
                gu.debugLog('Posted initial item (to prime websocket) ', DEBUG);
            });

//            mainTest();
        };

        var ws = dhh.createWebSocket(wsUri, onOpen);

        ws.on('message', function(data, flags) {

            actualResponseQueue.push(data);
            gu.debugLog('Received message: '+ data, DEBUG);
            gu.debugLog('Response queue length: '+ actualResponseQueue.length, DEBUG);

            if (1 == actualResponseQueue.length) {
                // This was the first item, so websocket is primed and ready.
                mainTest();
            } else if (actualResponseQueue.length == numUpdates) {
                confirmOrderOfResponses();
            }
        });
    });

    // SKIP - this should be run on demand, not automatically
    it.skip('Continuous updates at 1 per 200 ms are sent with order preserved.', function(done) {
        var actualResponseQueue = [], expectedResponseQueue = [], endWait, i;
        var numUpdates = 500,
            waitBetween = 200,
            first_post_time = null,
            last_post_time = null,
            time_to_post_all = null,
            VERBOSE = true;
        this.timeout((numUpdates * WAIT_FOR_CHANNEL_RESPONSE_MS) + 45000);

        var mainTest = function() {
            first_post_time = moment();
            async.timesSeries(numUpdates, function(n, next) {
                dhh.postData({channelUri: channelUri, data: dhh.getRandomPayload()}, function(res, uri) {
                    gu.debugLog('Posted data #'+ n, DEBUG);
                    setTimeout(function() {
                        next(null, uri);
                    }, waitBetween);
                });
            }, function(err, uris) {
                // pass
            });
        };

        // Confirms order of responses and then calls confirmAllRelativeLinks(), which ends test
        var confirmOrderOfResponses = function() {
            last_post_time = moment();
            time_to_post_all = last_post_time.diff(first_post_time, 'seconds');
            gu.debugLog('Post time for '+ numUpdates +' items: '+ time_to_post_all +' seconds.')

            gu.debugLog('...entering confirmOrderOfResponses()');

            dhh.getListOfLatestUrisFromChannel({numItems: numUpdates, channelUri: channelUri}, function(allUris) {
                expectedResponseQueue = allUris;
                gu.debugLog('Expected response queue length: '+ expectedResponseQueue.length, DEBUG);

                expect(actualResponseQueue.length).to.equal(numUpdates);
                expect(expectedResponseQueue.length).to.equal(numUpdates);

                gu.debugLog('Expected and Actual queues are full. Comparing queues...', DEBUG);

                for (i = 0; i < numUpdates; i += 1) {
                    //expect(actualResponseQueue[i]).to.equal(expectedResponseQueue[i]);
                    expect(doUrlsMatch({urlA: actualResponseQueue[i], urlB: expectedResponseQueue[i]})).to.be.true;
                    gu.debugLog('Matched queue number '+ i, DEBUG);
                }

                confirmAllRelativeLinks();

            });
        }

        // Test next/prev for each uri, as well as latest for channel. Then end test.
        var confirmAllRelativeLinks = function() {
            gu.debugLog('...entering confirmAllRelativeLinks()');

            dhh.testRelativeLinkInformation({channelUri: channelUri, numItems: numUpdates, debug: VERBOSE}, function(err) {
                if (null != err) {

                    gu.debugLog('Error in relative links test: '+ err);
                    expect(err).to.be.null;
                }

                ws.close();

                gu.debugLog('Post time for '+ numUpdates +' items: '+ time_to_post_all +' seconds.')

                done();
            })
        }

        var onOpen = function() {
            gu.debugLog('Open event fired!', DEBUG);
            gu.debugLog('Readystate: '+ ws.readyState, DEBUG);
            mainTest();
        };

        var ws = dhh.createWebSocket(wsUri, onOpen);

        ws.on('message', function(data, flags) {
            actualResponseQueue.push(data);
            gu.debugLog('Received message: '+ data, DEBUG);
            gu.debugLog('Response queue length: '+ actualResponseQueue.length, DEBUG);

            if (actualResponseQueue.length == numUpdates) {
                confirmOrderOfResponses();
            }
        });
    });

    // SKIP - this should be run on demand, not automatically
    it.skip('Continuous updates of 8MB at 1 per minute are sent with order preserved.', function(done) {
        var actualResponseQueue = [], expectedResponseQueue = [], endWait, i;
        var numUpdates = 500,
            waitBetween = 60 * 1000,
            first_post_time = null,
            last_post_time = null,
            time_to_post_all = null,
            est_time_to_process_each_item = 30 * 1000,
            VERBOSE = true;

        this.timeout((numUpdates * (WAIT_FOR_CHANNEL_RESPONSE_MS + waitBetween + est_time_to_process_each_item)) + 45000);

        var mainTest = function() {
            var payload = fs.readFileSync(MY_8MB_FILE, "utf8");

            first_post_time = moment();

            async.timesSeries(numUpdates, function(n, next) {
                dhh.postData({channelUri: channelUri, data: payload}, function(res, uri) {
                    expect(gu.isHTTPSuccess(res.status)).to.equal(true);

                    dhh.confirmExpectedData(uri, payload, function(didMatch) {
                        expect(didMatch).to.be.true;

                        gu.debugLog('Confirmed data post number '+ (n + 1) +' after '+ moment().diff(first_post_time, 'minutes') +' minutes');

                        setTimeout(function() {
                            next(null, uri);
                        }, waitBetween);
                    });
                })

                /*
                dhh.postData({channelUri: channelUri, data: dhh.getRandomPayload()}, function(res, uri) {
                    gu.debugLog('Posted data #'+ n, DEBUG);
                    setTimeout(function() {
                        next(null, uri);
                    }, waitBetween);
                });
                */
            }, function(err, uris) {
                // pass
            });
        };

        // Confirms order of responses and then calls confirmAllRelativeLinks(), which ends test
        var confirmOrderOfResponses = function() {
            last_post_time = moment();
            time_to_post_all = last_post_time.diff(first_post_time, 'seconds');
            gu.debugLog('Post time for '+ numUpdates +' items: '+ time_to_post_all +' seconds.')

            gu.debugLog('...entering confirmOrderOfResponses()');

            dhh.getListOfLatestUrisFromChannel({numItems: numUpdates, channelUri: channelUri}, function(allUris) {
                expectedResponseQueue = allUris;
                gu.debugLog('Expected response queue length: '+ expectedResponseQueue.length, DEBUG);

                expect(actualResponseQueue.length).to.equal(numUpdates);
                expect(expectedResponseQueue.length).to.equal(numUpdates);

                gu.debugLog('Expected and Actual queues are full. Comparing queues...', DEBUG);

                for (i = 0; i < numUpdates; i += 1) {
                    //expect(actualResponseQueue[i]).to.equal(expectedResponseQueue[i]);
                    expect(doUrlsMatch({urlA: actualResponseQueue[i], urlB: expectedResponseQueue[i]})).to.be.true;
                    gu.debugLog('Matched queue number '+ i, DEBUG);
                }

                confirmAllRelativeLinks();

            });
        }

        // Test next/prev for each uri, as well as latest for channel. Then end test.
        var confirmAllRelativeLinks = function() {
            gu.debugLog('...entering confirmAllRelativeLinks()');

            dhh.testRelativeLinkInformation({channelUri: channelUri, numItems: numUpdates, debug: VERBOSE}, function(err) {
                if (null != err) {

                    gu.debugLog('Error in relative links test: '+ err);
                    expect(err).to.be.null;
                }

                ws.close();

                gu.debugLog('Post time for '+ numUpdates +' items: '+ time_to_post_all +' seconds.')

                done();
            })
        }

        var onOpen = function() {
            gu.debugLog('Open event fired!', DEBUG);
            gu.debugLog('Readystate: '+ ws.readyState, DEBUG);
            mainTest();
        };

        var ws = dhh.createWebSocket(wsUri, onOpen);
        gu.debugLog('  -- Using channel: '+ wsUri)

        ws.on('message', function(data, flags) {
            actualResponseQueue.push(data);
            gu.debugLog('Received message: '+ data, DEBUG);
            gu.debugLog('Response queue length: '+ actualResponseQueue.length, DEBUG);

            if (actualResponseQueue.length == numUpdates) {
                confirmOrderOfResponses();
            }
        });
    });

    it('Multiple agents on a channel can be supported.', function(done) {
        // Channel created
        // create twelve agents that subscribe to the channel
        // channel pumps out two bits of data
        // each channel receives data in correct order
        var sockets = [],
            numAgents = 12,
            numOpenSockets = 0,
            numPrimedSockets = 0,
            uri1,   // remember, the numbers do NOT necessarily reflect the order of creation
            uri2;


        // Called from newSocketIsReady() if all sockets are ready
        var postItemsToChannel = function() {
            // Post TWO messages to channel
            async.parallel([
                function(callback){
                    dhh.postData({channelUri: channelUri, data: dhh.getRandomPayload()}, function(res, uri) {
                        expect(gu.isHTTPSuccess(res.status)).to.equal(true);
                        gu.debugLog('Posted first value ', DEBUG);
                        uri1 = uri;
                        callback(null, null);
                    });
                },
                function(callback){
                    dhh.postData({channelUri: channelUri, data: dhh.getRandomPayload()}, function(res, uri) {
                        expect(gu.isHTTPSuccess(res.status)).to.equal(true);
                        gu.debugLog('Posted second value ', DEBUG);
                        uri2 = uri;
                        callback(null, null);
                    });
                }
            ],
                function(e, r){

                    // pass  (rewrote the stuff below and moved it out into testAllSockets() )
                });
        };

        // initial post for priming sockets
        var primeSockets = function() {
            dhh.postData({channelUri: channelUri, data: dhh.getRandomPayload()}, function(res, uri) {
                expect(gu.isHTTPSuccess(res.status)).to.equal(true);
                gu.debugLog('Posted priming value ', DEBUG);
            });
        }

        // Called from afterOnMessage() if all sockets have received messages
        var testAllSockets = function() {
            dhh.getLatestUri(channelUri, function(latestUri) {
                var firstUri = (latestUri == uri1) ? uri2 : uri1;

                expect(numFullSockets()).to.equal(numAgents);

                for (var i = 0; i < sockets.length; i += 1) {
                    var thisSocket = sockets[i];

                    expect(thisSocket.responseQueue.length).to.equal(2);

                    //expect(thisSocket.responseQueue[0]).to.equal(firstUri);
                    //expect(thisSocket.responseQueue[1]).to.equal(latestUri);

                    expect(doUrlsMatch({urlA: thisSocket.responseQueue[0], urlB: firstUri})).to.be.true;
                    expect(doUrlsMatch({urlA: thisSocket.responseQueue[1], urlB: latestUri})).to.be.true;


                    gu.debugLog('Final socket state for socket '+ thisSocket.name +' is '+ socket.ws.readyState, DEBUG);

                    thisSocket.ws.close();
                }

                done();
            });
        }

        // Called when each socket is opened -- if all are opened, then prime them.
        var newSocketIsOpen = function() {
            numOpenSockets += 1;
            if (numAgents === numOpenSockets) {
                primeSockets();
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

        // Called when a socket's onMessage() is done
        // Hacky: would be ideal to check which socket is primed, but for now just counting total number.
        var afterOnMessage = function() {

            // If the last socket waiting to be primed is primed, then post the "real" items
            if (numPrimedSockets < numAgents) {
                numPrimedSockets += 1;
//                gu.debugLog(numPrimedSockets.toString() +' sockets are now primed (waiting for all '+ numAgents +').')

                if (numAgents == numPrimedSockets) {
//                    gu.debugLog('OK. All sockets are primed. Clearing their responseQueues...')

                    // Clear out each socket's responseQueue (since the item posted was just to prime it)
                    for (var i = 0; i < sockets.length; i += 1) {
                        sockets[i].responseQueue = [];
                    }

//                    gu.debugLog('All responseQueues cleared. Now calling the function to post the items.')

                    postItemsToChannel();
                }
            }

            if (numFullSockets() == numAgents) {
                testAllSockets();
            }
        }

        // Create yon sockets
        for (var i = 0; i < numAgents; i += 1) {
            var socket = new dhh.WSWrapper({
                'domain': DOMAIN,
                'uri': wsUri,
                'socketName': 'ws_'+ i,
                'onOpenCB': newSocketIsOpen,
                'onMessageCB': afterOnMessage
            });

            socket.createSocket();
            sockets[i] = socket;
        }
    });

    it('Disconnecting and then adding a new agent on same channel works.', function(done) {
        var socket1, socket2, uri1, uri2;

        var socket1OnOpen = function() {
            // broadcast message; confirm socket 1 received
            gu.debugLog('...entering socket1 Open function', DEBUG);

            dhh.postData({channelUri: channelUri, data: dhh.getRandomPayload()}, function(res, uri) {
                expect(gu.isHTTPSuccess(res.status)).to.equal(true);
                gu.debugLog('Posted first value ', DEBUG);
                uri1 = uri;
            });

        };

        var socket2OnOpen = function() {
            gu.debugLog('...entering socket2 Open function', DEBUG);

            expect(socket2.responseQueue.length).to.equal(0);

            dhh.postData({channelUri: channelUri, data: ranU.randomString(50)}, function(res, uri) {
                expect(gu.isHTTPSuccess(res.status)).to.equal(true);
                gu.debugLog('Posted second value ', DEBUG);
                uri2 = uri;
            });
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
            //expect(uri1).to.equal(socket1.responseQueue[0]);
            expect(doUrlsMatch({urlA: uri1, urlB: socket1.responseQueue[0]})).to.be.true;
            gu.debugLog('Confirmed first socket received msg', DEBUG);

            expect(socket2.responseQueue.length).to.equal(0);   // socket2 not connected yet
            gu.debugLog('Confirmed second socket queue is empty', DEBUG);

            gu.debugLog('Calling socket1.close()', DEBUG);
            socket1.ws.close();
            gu.debugLog('About to create second socket', DEBUG);
            socket2.createSocket();

            socket2.ws.on('close', function(code, message) {
                gu.debugLog('Socket2 closed', DEBUG);
                done();
            });

        }

        var finishSocket2 = function() {
            gu.debugLog('...entering finishSocket2()');

            expect(socket2.responseQueue.length).to.equal(1);
            //expect(uri2).to.equal(socket2.responseQueue[0]);
            expect(doUrlsMatch({urlA: uri2, urlB: socket2.responseQueue[0]})).to.be.true;
            gu.debugLog('Confirmed second socket received msg', DEBUG);

            socket2.ws.close();
        }

        socket1 = new dhh.WSWrapper({
            'domain': DOMAIN,
            'uri': wsUri,
            'socketName': 'ws_1',
            'onOpenCB': socket1OnOpen,
            'onMessageCB': socket1Msg
        });

        socket2 = new dhh.WSWrapper({
            'domain': DOMAIN,
            'uri': wsUri,
            'socketName': 'ws_2',
            'onOpenCB': socket2OnOpen,
            'onMessageCB': socket2Msg
        });

        socket1.createSocket();

        socket1.ws.on('close', function(code, message) {
            gu.debugLog('Socket1 closed', DEBUG);
        });
    });

    it('if one of two agents disconnects, the other will still get messages', function(done) {
        var fickleSocket,
            patientSocket,  // sockettomeh
            numReadySockets = 0,
            postedUri,
            VERBOSE = true;

        // Called when each socket is ready.
        var newSocketIsReady = function() {
            gu.debugLog('...in newSocketIsReady()', VERBOSE);
            numReadySockets += 1;
            if (2 === numReadySockets) {
                fickleSocket.ws.close();
                gu.debugLog('Calling close() on fickle socket', VERBOSE);
            }
        };

        // Called after message event has been handled
        var afterOnMessage = function() {
            gu.debugLog('...in afterOnMessage()', VERBOSE);

            if ('undefined' != typeof postedUri) {
                finishTest();
            }
            // else, wait for the postData call to call finishTest()
        }

        // Because we cannot be sure whether the postData call will complete before the message event has fired, code
        //  for each of those cases will check if both conditions have been met and then call this.
        var finishTest = function() {
            expect(patientSocket.responseQueue.length).to.equal(1);
            //expect(patientSocket.responseQueue[0]).to.equal(postedUri);
            expect(doUrlsMatch({urlA: patientSocket.responseQueue[0], urlB: postedUri})).to.be.true;
            gu.debugLog('Message received', VERBOSE);

            done();
        }

        // Create both sockets
        fickleSocket = new dhh.WSWrapper({
            'domain': DOMAIN,
            'uri': wsUri,
            'socketName': 'fickle',
            'onOpenCB': newSocketIsReady,
            'onMessageCB': null
        });

        patientSocket = new dhh.WSWrapper({
            'domain': DOMAIN,
            'uri': wsUri,
            'socketName': 'patient',
            'onOpenCB': newSocketIsReady,
            'onMessageCB': afterOnMessage
        });

        patientSocket.createSocket();
        fickleSocket.createSocket();

        fickleSocket.ws.on('close', function(code, message) {
            gu.debugLog('Fickle socket closed', VERBOSE);

            dhh.postData({channelUri: channelUri, data: dhh.getRandomPayload()}, function(res, uri) {
                expect(gu.isHTTPSuccess(res.status)).to.equal(true);
                gu.debugLog('Posted value ', VERBOSE);
                postedUri = uri;

                if (patientSocket.responseQueue.length > 0) {
                    finishTest();
                }
            });
        })
    })

    it.skip('<NOT WRITTEN> Multiple agents on multiple channels is handled appropriately.', function(done) {
        done();
    });


});
