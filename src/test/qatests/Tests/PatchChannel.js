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

describe.skip('SKIP - update TTL - Patch Channel', function() {

    /**
     * Creates a new channel and makes a PATCH call to it.
     * @param params: (ALL are optional) .name = original channel name,
     *  .ttlMillis = original TTL,
     *  .newTtlMillis = TTL sent via PATCH,
     *  .newName = name sent via PATCH
     * @param callback: dhh.channelMetadata object, patch response
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

            var channel = new dhh.channelMetadata(res.body);
            gu.debugLog('Test channel:'+ channel.getChannelUri(), DEBUG);

            patchPayload.channelUri = channel.getChannelUri();
            dhh.patchChannel(patchPayload, function(pRes) {
                channel = new dhh.channelMetadata(pRes.body);

                callback(channel, pRes);
            })
        });
    }

    describe('Acceptance - set channel with default TTL value to non-default TTL value', function() {
        var newTTL = ranU.randomNum(50000) + 10000,
            patchRes = null,
            channel = null;

        before(function(callback) {
            createAndPatchChannel({newTtlMillis: newTTL}, function(cnMetadata, pRes) {
                patchRes = pRes;
                channel = cnMetadata;

                callback();
            })
        })

        it('returns 200', function() {
            expect(patchRes.status).to.equal(gu.HTTPresponses.OK);
        })

        it('returns updated TTL', function() {
            expect(channel.getTTL()).to.equal(newTTL);
        })
    })

    describe('Acceptance - set channel with no TTL value to have a TTL value', function() {
        var newTTL = ranU.randomNum(50000) + 10000,
            patchRes = null,
            channel = null;

        before(function(callback) {
            createAndPatchChannel({ttlMillis: null, newTtlMillis: newTTL}, function(cnMetadata, pRes) {
                patchRes = pRes;
                channel = cnMetadata;

                callback();
            })
        })

        it('returns 200', function() {
            expect(patchRes.status).to.equal(gu.HTTPresponses.OK);
        })

        it('returns updated TTL', function() {
            expect(channel.getTTL()).to.equal(newTTL);
        })
    })

    describe('Other scenarios', function() {

        // BUG: https://www.pivotaltracker.com/story/show/52756553
        it('can set channel with default TTL value to have no TTL value', function(done) {
            createAndPatchChannel({newTtlMillis: null}, function(channel, pRes) {
                expect(pRes.status).to.equal(gu.HTTPresponses.OK);
                expect(typeof channel.getTTL()).to.equal('undefined');

                done();
            })
        })

        it('can PATCH with same TTL value, resulting in 200', function(done) {
            createAndPatchChannel({newTtlMillis: dhh.DEFAULT_TTL}, function(channel, pRes) {
                expect(pRes.status).to.equal(gu.HTTPresponses.OK);
                expect(channel.getTTL()).to.equal(dhh.DEFAULT_TTL);

                done();
            })
        })

        it('can update TTL twice', function(done) {
            var firstUpdatedTTL = 25000,
                secondUpdatedTTL = 35000;

            createAndPatchChannel({newTtlMillis: firstUpdatedTTL}, function(channel, pRes) {
                expect(pRes.status).to.equal(gu.HTTPresponses.OK);
                expect(channel.getTTL()).to.equal(firstUpdatedTTL);

                dhh.patchChannel({channelUri: channel.getChannelUri(), ttlMillis: secondUpdatedTTL},
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

        it('returns 400 when trying to change channel name', function(done) {
            var payload = {
                name: dhh.getRandomChannelName(),
                newName: dhh.getRandomChannelName(),
                newTtlMillis: 8675309
            };

            createAndPatchChannel(payload, function(channel, pRes) {
                expect(gu.isHTTPError(pRes.status)).to.be.true;

                done();
            })
        })

        it('returns 404 for channel that does not exist', function(done) {
            dhh.patchChannel({channelUri: dhh.FAKE_CHANNEL_URI, ttlMillis: 8675309}, function(patchRes) {
                expect(patchRes.status).to.equal(gu.HTTPresponses.Not_Found);

                done();
            })
        })

        describe.skip('TODO: low-pri error cases', function() {
            // negative TTL

            // alpha characters

            // TTL is zero

            // TTL contains a period

            // TTL is blank or empty = ?
        })
    })
})

