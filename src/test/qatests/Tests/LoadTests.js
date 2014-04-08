/**
 * Created with JetBrains WebStorm.
 * User: gnewcomb
 * Date: 3/1/13
 * Time: 9:47 AM
 * To change this template use File | Settings | File Templates.
 */

// Load tests

var chai = require('chai');
var expect = chai.expect;
var superagent = require('superagent');
var crypto = require('crypto');
var request = require('request');
var moment = require('moment');
var async = require('async'),
    lodash = require('lodash')
    fs = require('fs');

var dhh = require('.././DH_test_helpers/DHtesthelpers.js'),
    ranU = require('../randomUtils.js'),
    gu = require('../genericUtils.js');

var MY_4MB_FILE = './artifacts/Iam4Mb.txt',
    MY_8MB_FILE = './artifacts/Iam8Mb.txt',
    MY_16MB_FILE = './artifacts/Iam16Mb.txt',
    MY_32MB_FILE = './artifacts/Iam32Mb.txt',
    MY_64MB_FILE = './artifacts/Iam64Mb.txt';

var URL_ROOT = dhh.URL_ROOT;

var channelName,
    channelUri;

describe('Load tests - POST data:', function(){

    var loadChannels = {},
        loadChannelKeys = [];  // channel.uri (to fetch data) and channel.data, e.g. { con {uri: x, data: y}}

    var postAndConfirmBigFile = function(fileLocation, callback) {
        var payload = fs.readFileSync(fileLocation, "utf8");

        dhh.postData({channelUri: channelUri, data: payload}, function(res, uri) {
            expect(gu.isHTTPSuccess(res.status)).to.equal(true);

            dhh.confirmExpectedData(uri, payload, function(didMatch) {
                expect(didMatch).to.be.true;

                callback();
            });
        });
    }

    /**
     *
     * @param params: .cnUri (channel URI),
     *  .numPosts || .timeToPost (milliseconds) -- only one of these,
     *  .postWaitTime=100 (milliseconds between each post attempt),
     *  .doBailOnError=true (if false, will continue trying to post / confirm),
     *  .debug
     * @param callback: statistics object (.total, .passes, .failures)
     */
    var multiplePostAndConfirm = function(params, callback) {
        var cnUri = params.cnUri,
            numPosts = (undefined !== params.numPosts) ? params.numPosts : null,
            timeToPost = (undefined != params.timeToPost) ? params.timeToPost : null,
            isTimed = (null == numPosts),
            postWaitTime = (undefined !== params.postWaitTime) ? params.postWaitTime : 100,
            doBailOnError = (undefined !== params.doBailOnError) ? params.doBailOnError: true,
            VERBOSE = (undefined !== params.debug) ? params.debug : true;

        if ((null == numPosts) && (null == timeToPost)) {
            throw new Error('Either numPosts or timeToPost must be provided in function call.');
        }

        var statistics = {
                total: 0,
                passes: 0,
                failures: 0
            },
            expiration = moment();

        if (isTimed) {
            expiration.add('milliseconds', timeToPost);
        }

        /*
        if (VERBOSE) {
            gu.debugLog('Variable dump.');
            gu.debugLog('numPosts: '+ numPosts);
            gu.debugLog('timeToPost: '+ timeToPost);
            gu.debugLog('isTimed: '+ isTimed);
            gu.debugLog('postWaitTime: '+ postWaitTime);
            gu.debugLog('doBailOnError: '+ doBailOnError);
        }
        */

        async.whilst(
            function() {
                if (isTimed) {
                    return moment().isBefore(expiration);
                }
                else {
                    return statistics.total < numPosts;
                }
            },
            function(cb) {
                //gu.debugLog('Going to wait '+ postWaitTime +' milliseconds to post next', VERBOSE);
                setTimeout(function() {
                    //gu.debugLog('Calling postAndConfirm()...', VERBOSE);
                    postAndConfirm({cnUri: cnUri, debug: VERBOSE}, function(result) {

                        // Update stats
                        if (result) {
                            statistics.passes += 1;
                        }
                        else {
                            statistics.failures += 1;
                        }
                        statistics.total += 1;

                        // Callback with result
                        if (doBailOnError && !result) {
                            cb('Failed; see output.');
                        }
                        else {
                            cb();
                        }
                    });
                }, postWaitTime);
            },
            function(err) {
                if (err) {
                    gu.debugLog(err);
                }

                callback(statistics);
            }
        )
    }

    // returns true if post and confirm passed, else false
    var postAndConfirm = function(params, callback) {
        var cnUri = params.cnUri,
            data = dhh.getRandomPayload(),
            VERBOSE = false;

        dhh.postData({channelUri: cnUri, data: data}, function(postRes, theDataUri) {
            if (!gu.isHTTPSuccess(postRes.status)) {
                gu.debugLog('Failed to INSERT data for channel '+ cnUri +' at '+ moment().format());
                gu.debugLog('Status: '+ postRes.status);
                gu.debugLog('Full text: '+ postRes.text);

                callback(false);
            }
            else {
                gu.debugLog('INSERTED data at: '+ theDataUri, VERBOSE);
                gu.debugLog('About to confirm data at :'+ theDataUri, VERBOSE);

                dhh.confirmExpectedData(theDataUri, data, function(didMatch){
                    if (didMatch) {
                        gu.debugLog('CONFIRMED data with GET at: '+ theDataUri, VERBOSE);
                    }
                    else {
                        gu.debugLog('FAILED to confirm data with GET at '+ theDataUri +' at '+ moment().format());
                    }

                    callback(didMatch);
                })
            }
        })
    }

    /**
     * Called at the end of various tests here that use the 'statistics' object, which just collects counts of
     *  test results (it is created in the multiplePostAndConfirm() function).
     * @param stats: the statistics object
     */
    var reportResults = function(stats) {
        var pctPass = Math.round((stats.passes / stats.total) * 100),
            pctFail = Math.round((stats.failures / stats.total) * 100);

        gu.debugLog('\n'+ stats.total +' total attempts to post and confirm an item.');
        gu.debugLog(stats.passes +' passing cases ('+ pctPass +'% pass rate.)');
        gu.debugLog(stats.failures +' failing cases ('+ pctFail +'% fail rate.)');
    }

    before(function(myCallback){
        channelName = dhh.getRandomChannelName();

        dhh.createChannel({name: channelName}, function(res, cnUri){
            if ((res.error) || (!gu.isHTTPSuccess(res.status))) {
                myCallback(res.error);
            };

            channelUri = cnUri;
            gu.debugLog('Main test channel:'+ channelName);

            myCallback();
        });
    });

    describe('Rapid data posting:', function() {
        // To ignore the Loadtest cases:  mocha -R nyan --timeout 4000 --grep Load --invert

        it.skip('Create multiple channels, post to each one, confirm data in each - limited by number of posts', function(done){
            var numChannels = 30,
                numPostsPerChannel = 100,
                calculatedTimeout = numChannels * ((numPostsPerChannel * 100) + 5000),
                allStats = {
                    total: 0,
                    passes: 0,
                    failures: 0
                },
                VERBOSE = true;

            this.timeout(calculatedTimeout);
            gu.debugLog('Timeout will be '+ calculatedTimeout +' milliseconds.');

            for (var i = 1; i <= numChannels; i++)
            {
                var thisName = dhh.getRandomChannelName(),
                    thisPayload = dhh.getRandomPayload();

                loadChannels[thisName] = {
                    channelUri: null,
                    dataUri: null,
                    data: thisPayload
                };

            }

            loadChannelKeys = lodash.keys(loadChannels);

            async.each(loadChannelKeys,
                function(cnName, cb) {

                    dhh.createChannel({name: cnName}, function(createRes, cnUri) {
                        if (gu.HTTPresponses.Created != createRes.status) {
                            gu.debugLog('Failed to create channel "'+ cnName +'"');

                            cb(null);
                        }

                        loadChannels[cnName].channelUri = cnUri;
                        expect(createRes.status).to.equal(gu.HTTPresponses.Created);
                        gu.debugLog('CREATED new channel at: '+ cnUri, VERBOSE);

                        var params = {
                            cnUri: cnUri,
                            numPosts: numPostsPerChannel,
                            postWaitTime: 0,
                            doBailOnError: false,
                            debug: VERBOSE
                        };

                        multiplePostAndConfirm(params, function(stats) {
                            allStats.total += stats.total;
                            allStats.passes += stats.passes;
                            allStats.failures += stats.failures;

                            cb(null);
                        });
                    })
                },
                function(err) {
                    expect(err).to.be.null;
                    reportResults(allStats);

                    done();
                }
            );

        });

        it('Create multiple channels, post to each one, confirm data in each - limited by time', function(done){
            var numChannels = 30,
                timeToPostSec = 120,
                calculatedTimeout = (numChannels * 5000) + (timeToPostSec * 1000),
                postWaitTime = 0,   // time to wait between posts on a given channel
                allStats = {
                    total: 0,
                    passes: 0,
                    failures: 0
                },
                doBailOnError = false,
                VERBOSE = true;

            this.timeout(calculatedTimeout);
            gu.debugLog('Timeout will be '+ calculatedTimeout +' milliseconds.');

            for (var i = 1; i <= numChannels; i++)
            {
                var thisName = dhh.getRandomChannelName(),
                    thisPayload = dhh.getRandomPayload();

                loadChannels[thisName] = {
                    channelUri: null,
                    dataUri: null,
                    data: thisPayload
                };

            }

            loadChannelKeys = lodash.keys(loadChannels);

            async.each(loadChannelKeys,
                function(cnName, cb) {

                    dhh.createChannel({name: cnName}, function(createRes, cnUri) {
                        if (gu.HTTPresponses.Created != createRes.status) {
                            gu.debugLog('Failed to create channel "'+ cnName +'" at '+ moment().format());
                            gu.debugLog('Status: '+ createRes.status);
                            gu.debugLog('Text: '+ createRes.text);

                            if (doBailOnError) {
                                cb('Failed to create channel.');
                            }
                            else {
                                cb();
                            }
                        }
                        else {
                            loadChannels[cnName].channelUri = cnUri;
                            gu.debugLog('CREATED new channel at: '+ cnUri, VERBOSE);

                            dhh.getChannel({uri: cnUri}, function(getCnRes) {
                                if (!gu.isHTTPSuccess(getCnRes.status)) {
                                    gu.debugLog('Failed to GET channel after creation. Status: '+ getCnRes.status);
                                    gu.debugLog('Channel URI: '+ cnUri);

                                    if (doBailOnError) {
                                        cb('Failed to get channel.');
                                    }
                                    else {
                                        cb();
                                    }
                                }
                                else {
                                    var params = {
                                        cnUri: cnUri,
                                        //numPosts: numPostsPerChannel,
                                        timeToPost: timeToPostSec * 1000,
                                        postWaitTime: postWaitTime,
                                        doBailOnError: doBailOnError,
                                        debug: VERBOSE
                                    };

                                    multiplePostAndConfirm(params, function(stats) {
                                        allStats.total += stats.total;
                                        allStats.passes += stats.passes;
                                        allStats.failures += stats.failures;

                                        if (0 == moment().unix() % 10) {
                                            gu.debugLog(allStats.total +' attempts so far.');
                                        }

                                        cb(null);
                                    });
                                }
                            })
                        }
                    })
                },
                function(err) {
                    if (null != err) {
                        gu.debugLog(err);
                    }
                    expect(err).to.be.null;
                    reportResults(allStats);
                    gu.debugLog('Posted for '+ timeToPostSec +' seconds on '+ numChannels +' new channels.');

                    done();
                }
            );

        });
    });

    describe.skip('Post big files:', function() {

        it('POST 2 MB file to channel', function(done) {
            postAndConfirmBigFile(MY_2MB_FILE, done);
        });

        it('POST 4 MB file to channel', function(done) {
            this.timeout(60000);

            postAndConfirmBigFile(MY_4MB_FILE, done);
        });

        it('POST 8 MB file to channel', function(done) {
            this.timeout(120000);

            postAndConfirmBigFile(MY_8MB_FILE, done);
        });

        it('POST 16 MB file to channel', function(done) {
            this.timeout(240000);

            var payload = fs.readFileSync(MY_16MB_FILE, "utf8");

            dhh.postData({channelUri: channelUri, data: payload}, function(res, uri) {
                expect(gu.HTTPresponses.Request_Entity_Too_Large).to.equal(res.status)

                done();
            });
        });
    });

    describe.skip('Unsupported scenarios (tests ignored):', function() {
        // as of 3/5/2012, DH cannot handle files this big
        it.skip('<UNSUPPORTED> POST and retrieve 32 MB file to channel', function(done) {
            this.timeout(480000);

            postAndConfirmBigFile(MY_32MB_FILE, done);
        });

        // as of 3/5/2012, DH cannot handle files this big
        it.skip('<UNSUPPORTED> POST and retrieve 64 MB file to channel', function(done) {
            this.timeout(960000);

            postAndConfirmBigFile(MY_64MB_FILE, done);
        });
    });
});