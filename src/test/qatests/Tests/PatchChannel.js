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

var DOMAIN = dhh.DOMAIN,
    DEBUG = true;

/*
    NOTE: This file only tests the updating of a channel's metadata. It does *not* test whether an item is correctly
        deleted when its TTL has been met. That is covered in the TTLDeletion.js file.
 */

describe('Update description', function() {
    var descChannelURI = null,
        altDescChannelURI = null,
        noDescChannelURI = null;

    // calls back with body of GET channel
    var patchAndGetChannel = function(params, cb) {
        dhh.patchChannel(params, function(res) {
            expect(res.status).to.equal(gu.HTTPresponses.OK);

            dhh.getChannel({'uri': params.channelUri}, function(getRes, body) {
                expect(getRes.status).to.equal(gu.HTTPresponses.OK);

                cb(body);
            })
        })
    }

    before(function(done) {
        var channelWithDescName = dhh.getRandomChannelName(),
        altChannelWithDescName = dhh.getRandomChannelName(),
        channelWODescName = dhh.getRandomChannelName();

        dhh.createChannel({'name': channelWithDescName, 'description': dhh.getRandomChannelDescription(),
            'domain': DOMAIN, 'debug': DEBUG}, function(res, newUri) {
                expect(res.status).to.equal(gu.HTTPresponses.Created);
                descChannelURI = newUri;

                dhh.createChannel({'name': channelWODescName, 'domain': DOMAIN, 'debug': DEBUG},
                    function(secondRes, secondUri) {
                        expect(secondRes.status).to.equal(gu.HTTPresponses.Created);
                        noDescChannelURI = secondUri;

                        dhh.createChannel({'name': altChannelWithDescName,
                                'description': dhh.getRandomChannelDescription(), 'domain': DOMAIN, 'debug': DEBUG},
                            function(thirdRes, thirdUri) {
                                expect(thirdRes.status).to.equal(gu.HTTPresponses.Created);
                                altDescChannelURI = thirdUri;

                                done();
                            })

                })
        })
    })

    it('Channel with description updated to empty desc', function(done) {
        patchAndGetChannel({'channelUri': descChannelURI, 'description':''}, function(body) {
            expect(body.description).to.equal('');

            done();
        })
    })

    it('Channel with no description updated to have desc', function(done) {
        var newDesc = dhh.getRandomChannelDescription();

        patchAndGetChannel({'channelUri': noDescChannelURI, 'description': newDesc}, function(body) {
            expect(body.description).to.equal(newDesc);

            done();
        })
    })

    it('Channel with description updated to null results in desc of empty string', function(done) {
        patchAndGetChannel({'channelUri': altDescChannelURI, 'description': null}, function(body) {
            expect(body.description).to.equal('');

            done();
        })
    })
})


