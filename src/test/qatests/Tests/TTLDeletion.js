/**
 * Created with IntelliJ IDEA.
 * User: gnewcomb
 * Date: 7/3/13
 * Time: 10:02 AM
 * To change this template use File | Settings | File Templates.
 */

var chai = require('chai'),
    expect = chai.expect,
    superagent = require('superagent'),
    request = require('request'),
    moment = require('moment'),
    async = require('async'),
    lodash = require('lodash');

var dhh = require('.././DH_test_helpers/DHtesthelpers.js'),
    gu = require('../genericUtils.js'),
    ranU = require('../randomUtils.js');

var SHORT_TTL = 5000,
    MEDIUM_TTL = 10000,
    INFINITE_TTL = 999999999,
    TTL_BUFFER = 1000,  // extra number of milliseconds beyond expiration to wait to run reap cycle
    DEBUG = true;

describe('TTL Deletion', function() {

    var cnMetadata = null;

    /**
     * Inserts an item and returns its metadata plus local expiration time.
     * @param params: .channel (channel metadata)
     * @param done: item metadata, item expiration time
     */
    var createItem = function(params, done) {
        var channel = params.channel,
            payload = {channelUri: channel.getChannelUri(), data: dhh.getRandomPayload()};

        dhh.postData(payload, function(response) {
            if (!gu.isHTTPSuccess(response.status)) {
                gu.debugLog('Error posting data: '+ response.status);
            }
            var iMetadata = new dhh.packetMetadata(response.body),
                expirationTime = getExpirationTime(channel, moment());

            done(iMetadata, expirationTime);
        })
    }

    /**
     * Execute reap cycle. When complete, call GET on the passed in item URI.
     * @param params: itemUri, debug (optional)
     * @param callback: reap response, GET response
     */
    var reapAndGetItem = function(params, callback) {
        var itemUri = params.itemUri,
            VERBOSE = (params.hasOwnProperty('debug')) ? (params.debug) : false;

        gu.debugLog('itemUri for reapAndGetItem(): '+ itemUri, DEBUG);

        dhh.executeTTLCleanup({debug: VERBOSE}, function(execRes) {
            dhh.getDataFromChannel({uri: itemUri, debug: VERBOSE}, function(err, getRes, data) {
                getRes['status'] = getRes.statusCode;   // make it match SuperAgent's property

                callback(execRes, getRes);
            })
        })
    }


    var getExpirationTime = function(channelMetadata, creationMoment) {
        var cnTTL = (null == channelMetadata.getTTL()) ? INFINITE_TTL : channelMetadata.getTTL(),
            expiration = creationMoment.clone().add('milliseconds', cnTTL),
            VERBOSE = false;

        if (VERBOSE) {
            gu.debugLog('channel TTL: '+ channelMetadata.getTTL());
            gu.debugLog('creation moment: '+ creationMoment.format());
            gu.debugLog('expiration: '+ expiration.format());
        }

        return expiration;
    }

    before(function() {
        expect(TTL_BUFFER).to.be.above(0);
        expect(SHORT_TTL).to.be.above(0);
        expect(MEDIUM_TTL).to.be.above(SHORT_TTL);
        expect(INFINITE_TTL).to.be.above(MEDIUM_TTL);
        expect(TTL_BUFFER).to.be.below(MEDIUM_TTL - SHORT_TTL);
    })

    beforeEach(function(done) {
        dhh.createChannel({name: dhh.getRandomChannelName()}, function(res){
            if ((res.error) || (!gu.isHTTPSuccess(res.status))) {
                throw new Error(res.error);
            }

            cnMetadata = new dhh.channelMetadata(res.body);

            done();
        });
    })

    describe('Acceptance', function() {

        it('expired item is removed after reaping cycle', function(done) {
            var VERBOSE = false;

            dhh.patchChannel({channelUri: cnMetadata.getChannelUri(), ttlMillis: SHORT_TTL}, function(patchRes) {
                expect(gu.isHTTPSuccess(patchRes.status)).to.be.true;
                cnMetadata = new dhh.channelMetadata(patchRes.body);

                createItem({channel: cnMetadata}, function(itemMetadata, expiration) {
                    var now = moment(),
                        timeDiff = expiration.diff(now),
                        wait = timeDiff + TTL_BUFFER;

                    if (VERBOSE) {
                        gu.debugLog('Now: '+ now.format());
                        gu.debugLog('time diff: '+ timeDiff);
                        gu.debugLog('Wait length in ms: '+ wait);
                    }

                    setTimeout(function(){
                        reapAndGetItem({itemUri: itemMetadata.getPacketUri()}, function(reapRes, getRes) {
                            expect(reapRes.status).to.equal(gu.HTTPresponses.OK);
                            expect(getRes.status).to.equal(gu.HTTPresponses.Not_Found);

                            done();
                        })
                    }, wait
                    );

                })
            })
        })

        it('item is not removed by reaping cycle if its expiration has not been met', function(done) {
            var VERBOSE = false;

            dhh.patchChannel({channelUri: cnMetadata.getChannelUri(), ttlMillis: SHORT_TTL}, function(patchRes) {
                expect(gu.isHTTPSuccess(patchRes.status)).to.be.true;
                cnMetadata = new dhh.channelMetadata(patchRes.body);

                createItem({channel: cnMetadata}, function(itemMetadata, expiration) {
                    setTimeout(function() {
                        reapAndGetItem({itemUri: itemMetadata.getPacketUri()}, function(reapRes, getRes) {
                            expect(reapRes.status).to.equal(gu.HTTPresponses.OK);
                            expect(getRes.status).to.equal(gu.HTTPresponses.OK);

                            done();
                        })
                    }, TTL_BUFFER
                    );
                })
            })
        })

        it('channel with no TTL - reaping cycle has no effect', function(done) {
            var VERBOSE = false;

            dhh.patchChannel({channelUri: cnMetadata.getChannelUri(), ttlMillis: null}, function(patchRes) {
                expect(gu.isHTTPSuccess(patchRes.status)).to.be.true;
                cnMetadata = new dhh.channelMetadata(patchRes.body);

                createItem({channel: cnMetadata}, function(itemMetadata, expiration) {
                    setTimeout(function() {
                            reapAndGetItem({itemUri: itemMetadata.getPacketUri()}, function(reapRes, getRes) {
                                expect(reapRes.status).to.equal(gu.HTTPresponses.OK);
                                expect(getRes.status).to.equal(gu.HTTPresponses.OK);

                                done();
                            })
                        }, TTL_BUFFER
                    );
                })
            })
        })


    })

    describe('Interesting cases', function() {
        it('updating null TTL to non-null will result in expired item', function(done) {
            var VERBOSE = false;

            dhh.patchChannel({channelUri: cnMetadata.getChannelUri(), ttlMillis: null}, function(firstPatchRes) {
                expect(gu.isHTTPSuccess(firstPatchRes.status)).to.be.true;

                cnMetadata = new dhh.channelMetadata(firstPatchRes.body);

                createItem({channel: cnMetadata}, function(itemMetadata, firstExpiration) {

                    dhh.patchChannel({channelUri: cnMetadata.getChannelUri(), ttlMillis: SHORT_TTL}, function(secondPatchRes) {
                        expect(gu.isHTTPSuccess(secondPatchRes.status)).to.be.true;

                        cnMetadata = new dhh.channelMetadata(secondPatchRes.body);
                        var expiration = getExpirationTime(cnMetadata, moment());


                        var now = moment(),
                            timeDiff = expiration.diff(now),
                            wait = timeDiff + TTL_BUFFER;

                        setTimeout(function(){
                                reapAndGetItem({itemUri: itemMetadata.getPacketUri()}, function(reapRes, getRes) {
                                    expect(reapRes.status).to.equal(gu.HTTPresponses.OK);
                                    expect(getRes.status).to.equal(gu.HTTPresponses.Not_Found);

                                    done();
                                })
                            }, wait
                        );
                    })


                })
            })
        })

        it('existing item whose channel has increased TTL is not removed after first TTL has been met but second has not', function(done) {
            var VERBOSE = true;

            dhh.patchChannel({channelUri: cnMetadata.getChannelUri(), ttlMillis: SHORT_TTL}, function(firstPatchRes) {
                expect(gu.isHTTPSuccess(firstPatchRes.status)).to.be.true;

                cnMetadata = new dhh.channelMetadata(firstPatchRes.body);

                createItem({channel: cnMetadata}, function(itemMetadata, firstExpiration) {

                    dhh.patchChannel({channelUri: cnMetadata.getChannelUri(), ttlMillis: MEDIUM_TTL}, function(secondPatchRes) {
                        expect(gu.isHTTPSuccess(secondPatchRes.status)).to.be.true;

                        cnMetadata = new dhh.channelMetadata(secondPatchRes.body);

                        var now = moment(),
                            timeDiff = firstExpiration.diff(now),
                            wait = timeDiff + TTL_BUFFER;

                        setTimeout(function(){
                                gu.debugLog('Reaping at first timeout', VERBOSE);

                                reapAndGetItem({itemUri: itemMetadata.getPacketUri()}, function(reapRes, getRes) {
                                    expect(reapRes.status).to.equal(gu.HTTPresponses.OK);
                                    expect(getRes.status).to.equal(gu.HTTPresponses.OK);

                                    setTimeout(function() {
                                            gu.debugLog('Reaping at second timeout', VERBOSE);
                                            reapAndGetItem({itemUri: itemMetadata.getPacketUri()}, function(reapRes, getRes) {
                                                expect(reapRes.status).to.equal(gu.HTTPresponses.OK);
                                                expect(getRes.status).to.equal(gu.HTTPresponses.Not_Found);

                                                done();
                                            })
                                        }, (MEDIUM_TTL - SHORT_TTL)
                                    );
                                })
                            }, wait
                        );
                    })

                })
            })
        })

        it('existing item whose channel has decreased TTL is removed after new earlier TTL has been met but original later TTL has not', function(done) {
            var creationTime = null,
                VERBOSE = true;

            dhh.patchChannel({channelUri: cnMetadata.getChannelUri(), ttlMillis: MEDIUM_TTL}, function(firstPatchRes) {
                expect(gu.isHTTPSuccess(firstPatchRes.status)).to.be.true;

                cnMetadata = new dhh.channelMetadata(firstPatchRes.body);

                createItem({channel: cnMetadata}, function(itemMetadata, firstExpiration) {
                    creationTime = moment();

                    dhh.patchChannel({channelUri: cnMetadata.getChannelUri(), ttlMillis: SHORT_TTL}, function(secondPatchRes) {
                        expect(gu.isHTTPSuccess(secondPatchRes.status)).to.be.true;

                        cnMetadata = new dhh.channelMetadata(secondPatchRes.body);

                        var expiration = getExpirationTime(cnMetadata, creationTime),
                            now = moment(),
                            timeDiff = expiration.diff(now),
                            wait = timeDiff + TTL_BUFFER;

                        if (wait < 0) {
                            wait = 0;
                        }

                        setTimeout(function(){
                                // Ensure we haven't hit original expiration yet
                                expect(moment().isBefore(firstExpiration)).to.be.true;

                                reapAndGetItem({itemUri: itemMetadata.getPacketUri()}, function(reapRes, getRes) {
                                    expect(reapRes.status).to.equal(gu.HTTPresponses.OK);
                                    expect(getRes.status).to.equal(gu.HTTPresponses.Not_Found);

                                    done();
                                })
                            }, wait
                        );
                    })

                })
            })
        })

        it('expired item that has been removed is not reinstated after channel TTL is set to near infinite', function(done) {
            var VERBOSE = false;

            dhh.patchChannel({channelUri: cnMetadata.getChannelUri(), ttlMillis: SHORT_TTL}, function(firstPatchRes) {
                expect(gu.isHTTPSuccess(firstPatchRes.status)).to.be.true;
                cnMetadata = new dhh.channelMetadata(firstPatchRes.body);

                createItem({channel: cnMetadata}, function(itemMetadata, expiration) {
                    var now = moment(),
                        timeDiff = expiration.diff(now),
                        wait = timeDiff + TTL_BUFFER;

                    setTimeout(function(){
                            reapAndGetItem({itemUri: itemMetadata.getPacketUri()}, function(reapRes, getRes) {
                                expect(reapRes.status).to.equal(gu.HTTPresponses.OK);
                                expect(getRes.status).to.equal(gu.HTTPresponses.Not_Found);

                                dhh.patchChannel({channelUri: cnMetadata.getChannelUri(), ttlMillis: INFINITE_TTL}, function(secondPatchRes) {
                                    expect(gu.isHTTPSuccess(firstPatchRes.status)).to.be.true;

                                    // Check twice -- once now and once after five seconds
                                    dhh.getDataFromChannel({uri: itemMetadata.getPacketUri()}, function(err, getDataRes) {
                                        expect(err).to.be.null;
                                        expect(getDataRes.statusCode).to.equal(gu.HTTPresponses.Not_Found);

                                        setTimeout(function() {
                                                dhh.getDataFromChannel({uri: itemMetadata.getPacketUri()}, function(err, getDataRes) {
                                                    expect(err).to.be.null;
                                                    expect(getDataRes.statusCode).to.equal(gu.HTTPresponses.Not_Found);

                                                    done();
                                                })
                                            }, 5000
                                        );
                                    })
                                })
                            })
                        }, wait
                    );
                })
            })
        })

        it('if all remaining items in a channel expire and are removed, get latest returns 404', function(done) {
            var latestUri = '',
                cnUri = cnMetadata.getChannelUri(),
                VERBOSE = false;

            dhh.patchChannel({channelUri: cnUri, ttlMillis: SHORT_TTL}, function(patchRes) {
                expect(gu.isHTTPSuccess(patchRes.status)).to.be.true;
                cnMetadata = new dhh.channelMetadata(patchRes.body);
                latestUri = cnMetadata.getLatestUri();

                createItem({channel: cnMetadata}, function(itemMetadata, expiration) {
                    var now = moment(),
                        timeDiff = expiration.diff(now),
                        wait = timeDiff + TTL_BUFFER;

                    setTimeout(function(){
                            reapAndGetItem({itemUri: itemMetadata.getPacketUri()}, function(reapRes, getRes) {
                                expect(reapRes.status).to.equal(gu.HTTPresponses.OK);
                                expect(getRes.status).to.equal(gu.HTTPresponses.Not_Found);

                                superagent.agent().get(latestUri)
                                    .end(function(err, res) {
                                        expect(res.status).to.equal(gu.HTTPresponses.Not_Found);

                                        done();
                                    })
                            })
                        }, wait
                    );

                })
            })
        })

        it.skip('BUG: https://www.pivotaltracker.com/story/show/53508215 - if first item in a channel expires, but the following item has not, then following item should no longer have a prev link ', function(done) {
            var cnUri = cnMetadata.getChannelUri(),
                VERBOSE = true;

            dhh.patchChannel({channelUri: cnUri, ttlMillis: SHORT_TTL}, function(patchRes) {
                expect(gu.isHTTPSuccess(patchRes.status)).to.be.true;
                gu.debugLog('Updated channel TTL.', VERBOSE);

                cnMetadata = new dhh.channelMetadata(patchRes.body);

                createItem({channel: cnMetadata}, function(firstItemMetadata, firstItemExpiration) {
                    gu.debugLog('Created first item.', VERBOSE);
                    var now = moment(),
                        timeDiff = firstItemExpiration.diff(now),
                        wait = timeDiff + TTL_BUFFER;

                    // wait until expiration, then:
                    setTimeout(function(){
                        gu.debugLog('Waited for first item to expire.', VERBOSE);

                        // - create second item and confirm it has prev link
                        createItem({channel: cnMetadata}, function(secondItemMetadata, secondItemExpiration) {
                            gu.debugLog('Created second item.', VERBOSE);

                            superagent.agent().get(secondItemMetadata.getPacketUri())
                                .end(function(err, getRes1) {
                                    var pheader = new dhh.packetGETHeader(getRes1.headers);
                                    expect(pheader.getPrevious()).to.equal(firstItemMetadata.getPacketUri());

                                    // Reap and confirm first item is removed
                                    reapAndGetItem({itemUri: firstItemMetadata.getPacketUri()}, function(reapRes, getRes) {
                                        expect(reapRes.status).to.equal(gu.HTTPresponses.OK);
                                        expect(getRes.status).to.equal(gu.HTTPresponses.Not_Found);
                                        gu.debugLog('First item was removed via expiration.', VERBOSE);

                                        // Get second item and confirm previous header is gone
                                        superagent.agent().get(secondItemMetadata.getPacketUri())
                                            .end(function(err, getRes2) {
                                                expect(getRes2.status).to.equal(gu.HTTPresponses.OK);
                                                pheader = new dhh.packetGETHeader(getRes2.headers);
                                                expect(pheader.getPrevious()).to.be.null;

                                                done();
                                            })
                                    })

                                })
                        })
                    }, wait
                    );
                })
            })
        })
    })
})
