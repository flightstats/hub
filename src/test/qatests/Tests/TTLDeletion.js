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
    LONG_TTL = 10000,
    TTL_BUFFER = 1000,  // extra number of milliseconds beyond expiration to wait to run reap cycle
    DEBUG = true;

describe.skip('SKIP - need to update for new API - TTL Deletion', function() {

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
    // COMMENTING OUT FOR NOW:  in case we go back to this functionality, I'm just hiding this :)
    /*
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
    */

    var getExpirationTime = function(channelMetadata, creationMoment) {
        var expiration = creationMoment.clone().add('milliseconds', channelMetadata.getTTL()),
            VERBOSE = false;

        if (VERBOSE) {
            gu.debugLog('****\ngetExpirationTime()')
            gu.debugLog('channel TTL: '+ channelMetadata.getTTL());
            gu.debugLog('creation moment: '+ creationMoment.format());
            gu.debugLog('expiration: '+ expiration.format());
        }

        return expiration;
    }

    beforeEach(function(done) {
        dhh.createChannel({name: dhh.getRandomChannelName()}, function(res){
            if ((res.error) || (!gu.isHTTPSuccess(res.status))) {
                throw new Error(res.error);
            }

            cnMetadata = new dhh.channelMetadata(res.body);

            done();
        });
    })

    // This entire section is waiting for the story that enables on-demand kickoff of the TTL checker / reaper.
    describe('Acceptance', function() {

        it('expired item is removed after reaping cycle', function(done) {
            var newTTL = SHORT_TTL,
                VERBOSE = false;

            dhh.patchChannel({channelUri: cnMetadata.getChannelUri(), ttlMillis: newTTL}, function(patchRes) {
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
                        dhh.getDataFromChannel({uri: itemMetadata.getPacketUri(), debug: VERBOSE}, function(err, getRes, data) {
                            getRes['status'] = getRes.statusCode;   // make it match SuperAgent's property
                            expect(getRes.statusCode).to.equal(gu.HTTPresponses.Not_Found);

                            done();
                        })

                        /*
                        reapAndGetItem({itemUri: itemMetadata.getPacketUri()}, function(reapRes, getRes) {
                            expect(reapRes.status).to.equal(gu.HTTPresponses.OK);
                            expect(getRes.status).to.equal(gu.HTTPresponses.Not_Found);

                            done();
                        })
                        */
                    }, wait
                    );
                })
            })
        })

        it('item is not removed by reaping cycle if its expiration has not been met', function(done) {
            var newTTL = SHORT_TTL,
                VERBOSE = false;

            dhh.patchChannel({channelUri: cnMetadata.getChannelUri(), ttlMillis: newTTL}, function(patchRes) {
                expect(gu.isHTTPSuccess(patchRes.status)).to.be.true;
                cnMetadata = new dhh.channelMetadata(patchRes.body);

                createItem({channel: cnMetadata}, function(itemMetadata, expiration) {
                    dhh.getDataFromChannel({uri: itemMetadata.getPacketUri(), debug: VERBOSE}, function(err, getRes, data) {
                        expect(getRes.statusCode).to.equal(gu.HTTPresponses.OK);

                        done();
                    })

                    /*
                    reapAndGetItem({itemUri: itemMetadata.getPacketUri()}, function(reapRes, getRes) {
                        expect(reapRes.status).to.equal(gu.HTTPresponses.OK);
                        expect(getRes.status).to.equal(gu.HTTPresponses.OK);

                        done();
                    })
                    */

                })
            })
        })

        it('item is not removed by reaping cycle if channel has no TTL', function(done) {
            var newTTL = SHORT_TTL,
                VERBOSE = false;

            dhh.patchChannel({channelUri: cnMetadata.getChannelUri(), ttlMillis: null}, function(patchRes) {
                expect(gu.isHTTPSuccess(patchRes.status)).to.be.true;
                cnMetadata = new dhh.channelMetadata(patchRes.body);

                createItem({channel: cnMetadata}, function(itemMetadata, expiration) {
                    setTimeout(function() {
                        dhh.getDataFromChannel({uri: itemMetadata.getPacketUri(), debug: VERBOSE}, function(err, getRes, data) {
                            expect(getRes.statusCode).to.equal(gu.HTTPresponses.OK);

                            done();
                        })
                        /*
                        reapAndGetItem({itemUri: itemMetadata.getPacketUri()}, function(reapRes, getRes) {
                            expect(reapRes.status).to.equal(gu.HTTPresponses.OK);
                            expect(getRes.status).to.equal(gu.HTTPresponses.OK);

                            done();
                        })
                        */
                    }, newTTL + 1000);
                })
            })
        })

        it('Second item created with shorter TTL is reaped first, second is reaped when appropriate', function(done) {
            var secondTTL = SHORT_TTL,
                delta = 4000,
                firstTTL = secondTTL + delta,
                first_expiration = null,
                second_expiration = null,
                first_metadata = null,
                second_metadata = null,
                VERBOSE = false;

            dhh.patchChannel({channelUri: cnMetadata.getChannelUri(), ttlMillis: firstTTL}, function(patchRes) {
                expect(gu.isHTTPSuccess(patchRes.status)).to.be.true;
                cnMetadata = new dhh.channelMetadata(patchRes.body);

                async.series([
                    function(cb) {
                        gu.debugLog('Creating first item', VERBOSE);
                        createItem({channel: cnMetadata}, function(itemMetadata, theExpiration) {
                            first_metadata = itemMetadata;
                            first_expiration = theExpiration;
                            gu.debugLog('Original expiration at '+ first_expiration.format(), VERBOSE);
                            cb(null, null);
                        })
                    },
                    function(cb) {
                        dhh.patchChannel({channelUri: cnMetadata.getChannelUri(), ttlMillis: secondTTL}, function(patchRes) {
                            gu.debugLog('Updating channel to shorter TTL', VERBOSE);
                            var err = gu.isHTTPError(patchRes.status);
                            cnMetadata = new dhh.channelMetadata(patchRes.body);

                            cb(err, null);
                        })
                    },
                    // Create second item now that TTL is shorter
                    function(cb) {
                        gu.debugLog('Creating second item', VERBOSE);
                        createItem({channel: cnMetadata}, function(itemMetadata, theExpiration) {
                            second_metadata = itemMetadata;
                            second_expiration = theExpiration;
                            gu.debugLog('Second item expiration at '+ second_expiration.format(), VERBOSE);
                            cb(null, null);
                        })
                    },
                    // Wait through new short TTL expiration (plus 500 ms), and confirm second item is gone but first is not
                    function(cb) {
                        var myWait = second_expiration.diff(moment());
                        myWait = (myWait <= 500) ? 500 : myWait + 500;
                        gu.debugLog('Going to wait '+ myWait +' milliseconds and try to GET second item at its short TTL', VERBOSE);
                        setTimeout(function() {
                            dhh.getDataFromChannel({uri: second_metadata.getPacketUri(), debug: VERBOSE}, function(err, getRes, data) {
                                expect(getRes.statusCode).to.equal(gu.HTTPresponses.Not_Found);

                                dhh.getDataFromChannel({uri: first_metadata.getPacketUri(), debug: VERBOSE},
                                    function(err, getRes, data) {
                                        expect(getRes.statusCode).to.equal(gu.HTTPresponses.OK);

                                        cb(err, null);
                                    })
                            })
                        }, myWait);
                    },
                    // Wait through first item's TTL expiration and confirm item has been reaped
                    function(cb) {
                        var myWait = first_expiration.diff(moment());
                        myWait = (myWait <= 500) ? 500 : myWait + 500;
                        gu.debugLog('Going to wait '+ myWait +' milliseconds and reap at new TTL', VERBOSE);
                        setTimeout(function() {
                            dhh.getDataFromChannel({uri: first_metadata.getPacketUri(), debug: VERBOSE},
                                function(err, getRes, data) {
                                    expect(getRes.statusCode).to.equal(gu.HTTPresponses.Not_Found);

                                    cb(err, null);
                                })
                        }, myWait);
                    }
                    ],
                    function(err, results) {
                        expect(err).to.be.null;
                        done();
                    }
                )

            })
        })



        // TODO: confirm updating TTL to an earlier value is respected

        // TODO: confirm updating TTL to null is respected

        // TODO: confirm updating null TTL to non-null is respected

        // TODO: Decrease TTL and confirm item is removed after new TTL has been met but original time has not.

        // TODO: An item that has already been removed due to TTL is not restored after its channel's TTL is extended.
    })
})
