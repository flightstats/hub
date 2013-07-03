/**
 * Created with IntelliJ IDEA.
 * User: gnewcomb
 * Date: 6/26/13
 * Time: 3:25 PM
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

var DEBUG = true;

/*
    NOTE: This file only tests the updating of a channel's metadata. It does *not* test whether an item is correctly
        deleted when its TTL has been met. That is covered in the TTLDeletion.js file.
 */

describe.only('Patch Channel', function() {

    var getLightweightChannel = function(cnBody) {
        var cnMetadata = new dhh.channelMetadata(cnBody),
            channel = {
                name: cnMetadata.getName(),
                uri: cnMetadata.getChannelUri(),
                ttl: cnMetadata.getTTL()
            };

        return channel;
    };

    /**
     * Creates a new channel and makes a PATCH call to it.
     * @param params: (ALL are optional) .name = original channel name,
     *  .ttlMillis = original TTL,
     *  .newTtlMillis = TTL sent via PATCH,
     *  .newName = name sent via PATCH
     * @param callback: lightweight channel object (.uri, .name, .newName, .ttl, .newTtl), patch response
     */
    var createAndPatchChannel = function(params, callback) {
        var cnName = (params.hasOwnProperty('name')) ? params.name : dhh.getRandomChannelName(),
            createPayload = {name: cnName},
            patchPayload = {channelUri: null};

        if (params.hasOwnProperty('ttlMillis')) {
            createPayload['ttlMillis'] = params.ttlMillis;
        }
        if (params.hasOwnProperty('newTtlMillis')) {
            patchPayload['ttlMillis'] = params.newTtlMillis;
        }
        if (params.hasOwnProperty('newName')) {
            patchPayload['name'] = params.newName;
        }

        dhh.createChannel(createPayload, function(res){
            if ((res.error) || (!gu.isHTTPSuccess(res.status))) {
                throw new Error(res.error);
            }

            var channel = getLightweightChannel(res.body);
            gu.debugLog('Test channel:'+ channel.uri, DEBUG);

            patchPayload.channelUri = channel.uri;
            dhh.patchChannel(patchPayload, function(pRes) {
                var cnMetadata = new dhh.channelMetadata(pRes.body);
                channel['newName'] = cnMetadata.getName();
                channel['newTtl'] = cnMetadata.getTTL();

                callback(channel, pRes);
            })
        });
    }

    describe('Acceptance - set channel with default TTL value to non-default TTL value', function() {
        var newTTL = ranU.randomNum(50000) + 10000,
            patchRes = null;

        before(function(callback) {
            createAndPatchChannel({newTtlMillis: newTTL}, function(channel, pRes) {
                patchRes = pRes;

                callback();
            })
        })

        it('returns 200', function() {
            expect(patchRes.status).to.equal(gu.HTTPresponses.OK);
        })

        it('returns updated TTL', function() {
            var cnMetadata = new dhh.channelMetadata(patchRes.body);
            expect(cnMetadata.getTTL()).to.equal(newTTL);
        })
    })

    describe('Acceptance - set channel with no TTL value to have a TTL value', function() {
        var newTTL = ranU.randomNum(50000) + 10000,
            patchRes = null;

        before(function(callback) {
            createAndPatchChannel({ttlMillis: null, newTtlMillis: newTTL}, function(channel, pRes) {
                patchRes = pRes;

                callback();
            })
        })

        it('returns 200', function() {
            expect(patchRes.status).to.equal(gu.HTTPresponses.OK);
        })

        it('returns updated TTL', function() {
            var cnMetadata = new dhh.channelMetadata(patchRes.body);
            expect(cnMetadata.getTTL()).to.equal(newTTL);
        })
    })

    describe('Other scenarios', function() {

        // BUG: https://www.pivotaltracker.com/story/show/52756553
        it('can set channel with default TTL value to have no TTL value', function(done) {
            createAndPatchChannel({newTtlMillis: null}, function(channel, pRes) {
                expect(pRes.status).to.equal(gu.HTTPresponses.OK);
                expect(typeof channel.newTtl).to.equal('undefined');

                done();
            })
        })

        it('can PATCH with same TTL value, resulting in 200', function(done) {
            createAndPatchChannel({newTtlMillis: dhh.DEFAULT_TTL}, function(channel, pRes) {
                expect(pRes.status).to.equal(gu.HTTPresponses.OK);
                expect(channel.newTtl).to.equal(dhh.DEFAULT_TTL);

                done();
            })
        })

        it('can update TTL twice', function(done) {
            var firstUpdatedTTL = 25000,
                secondUpdatedTTL = 35000;

            createAndPatchChannel({newTtlMillis: firstUpdatedTTL}, function(channel, pRes) {
                expect(pRes.status).to.equal(gu.HTTPresponses.OK);
                expect(channel.newTtl).to.equal(firstUpdatedTTL);

                dhh.patchChannel({channelUri: channel.uri, ttlMillis: secondUpdatedTTL},
                    function(patchRes) {
                        expect(patchRes.status).to.equal(gu.HTTPresponses.OK);
                        var cnMetadata = new dhh.channelMetadata(patchRes.body);
                        expect(cnMetadata.getTTL()).to.equal(secondUpdatedTTL);

                        done();
                })
            })
        })
    })

    describe('Error / invalid cases', function() {
        // Try to change the channel name = bad request?

        // a channel name that does not exist returns 404

        // negative TTL

        // alpha characters

        // TTL is zero

        // TTL contains a period

        // TTL is blank or empty = ?
    })
})

