/**
 * Created with IntelliJ IDEA.
 * User: gnewcomb
 * Date: 6/13/13
 * Time: 8:30 AM
 * To change this template use File | Settings | File Templates.
 */

var chai = require('chai'),
    expect = chai.expect,
    superagent = require('superagent'),
    async = require('async'),
    lodash = require('lodash');


var dhh = require('../DH_test_helpers/DHtesthelpers.js'),
    ranU = require('../randomUtils.js'),
    gu = require('../genericUtils.js');

describe('Get All Channels', function() {
    var initialListOfChannels = [],
        OK = gu.HTTPresponses.OK,
        DEBUG = false;

    describe('Acceptance', function() {
        var latestChannel = {
            name: null,
            loc: null
            },
            getAllRes;

        before(function(done) {
            latestChannel.name = dhh.getRandomChannelName();

            dhh.createChannel({name: latestChannel.name}, function(createRes, channelLoc) {
                expect(createRes.status).to.equal(gu.HTTPresponses.Created);
                expect(null != channelLoc);
                latestChannel.loc = channelLoc;

                dhh.getAllChannels({}, function(res, theChannels) {
                    getAllRes = res;
                    initialListOfChannels = theChannels;
                    gu.debugLog('Found '+ initialListOfChannels.length +' channels.');

                    if (DEBUG) {
                        gu.debugLog('All channels: ');
                        console.dir(theChannels);
                    }

                    done();
                })
            })
        })

        it('returns OK', function() {
            expect(getAllRes.status).to.equal(OK);
        })

        it('returns at least one channel', function() {
            expect(initialListOfChannels.length).to.be.at.least(1);
        })

        it('returns the newly created channel', function() {
            for (var i = 0; i < initialListOfChannels.length; i += 1) {
                var channel = initialListOfChannels[i];
                if (channel.name == latestChannel.name) {
                    expect(channel.href).to.equal(latestChannel.loc);
                }
            }
        })

        // TODO: all valid channels are returned (how?)

        it('has the expected response structure', function() {
            var body = getAllRes.body;
            expect(body._links.self.hasOwnProperty('href')).to.be.true;
            expect(body._links.hasOwnProperty('channels')).to.be.true;
            expect(lodash.keys(body).length).to.equal(1);
            expect(lodash.keys(body._links).length).to.equal(2);
        })

        it('has the correct value for self link', function() {
            expect(getAllRes.body._links.self.href).to.equal([dhh.URL_ROOT, 'channel'].join('/'));
        })

        it('has right value for each each channel href and name', function(done) {
            var VERBOSE = true;

            var confirmChannelEntry = function(cnEntry, callback) {
                dhh.getChannel({uri: cnEntry.href}, function(getRes, body) {
                    expect(getRes.status).to.equal(OK);
                    expect(body.name).to.equal(cnEntry.name);
                    gu.debugLog('Confirmed channel '+ cnEntry.name);

                    callback();
                })
            }

            async.each(initialListOfChannels, confirmChannelEntry, function(err) {
                done();
            })
        })
    })
})