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
        var expiration = creationMoment.clone().add('milliseconds', channelMetadata.getTTL()),
            VERBOSE = false;

        if (VERBOSE) {
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

        // confirm GET returns item if expiration has not been met
        it('item is not removed by reaping cycle if its expiration has not been met', function(done) {
            var newTTL = SHORT_TTL,
                VERBOSE = false;

            dhh.patchChannel({channelUri: cnMetadata.getChannelUri(), ttlMillis: newTTL}, function(patchRes) {
                expect(gu.isHTTPSuccess(patchRes.status)).to.be.true;
                cnMetadata = new dhh.channelMetadata(patchRes.body);

                createItem({channel: cnMetadata}, function(itemMetadata, expiration) {
                    reapAndGetItem({itemUri: itemMetadata.getPacketUri()}, function(reapRes, getRes) {
                        expect(reapRes.status).to.equal(gu.HTTPresponses.OK);
                        expect(getRes.status).to.equal(gu.HTTPresponses.OK);

                        done();
                    })

                })
            })
        })

        // confirm GET returns item for channel with no TTL

        // confirm updating TTL to a later value is respected

        // confirm updating TTL to an earlier value is respected

        // confirm updating TTL to null is respected

        // confirm updating null TTL to non-null is respected

        // Increase TTL and confirm item is not removed after original TTL has been met but new time has not.

        // Decrease TTL and confirm item is removed after new TTL has been met but original time has not.

        // An item that has already been removed due to TTL is not restored after its channel's TTL is extended.
    })
})
