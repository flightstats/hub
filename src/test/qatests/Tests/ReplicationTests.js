/**
 * Created with IntelliJ IDEA.
 * User: gnewcomb
 * Date: 4/11/14
 * Time: 1:58 PM
 * To change this template use File | Settings | File Templates.
 */

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
    SOURCE_DOMAIN = 'hub.svc.staging',
    TARGET_DOMAIN = 'hub.svc.dev',
    DEBUG = true;


describe('Replication', function() {

    var INITIAL_SOURCE_CONFIG = null,       // The rep config for SOURCE_DOMAIN
        INITIAL_FULL_CONFIG = null,
        defaultChannelName = null;

    var getDefaultChannel = function(cb) {
        dhh.getAllChannels({'domain': SOURCE_DOMAIN, 'debug': DEBUG},
            function(res, all_channels) {
                expect(gu.isHTTPSuccess(res.status)).to.be.true;
                expect(all_channels.length).to.be.at.least(1);

                var selectedChannel = all_channels[lodash.random(0, all_channels.length -1)];

                cb(selectedChannel['name']);
            })
    }

    // Called to create a replication config from defaultChannelName if a config doesn't already exist on SOURCE
    var createBasicConfigEntry = function(cb) {

        dhh.createChannel({'name': defaultChannelName, 'domain': SOURCE_DOMAIN, 'debug': DEBUG},
            function(res, channelUri) {
                expect(gu.isHTTPSuccess(res.status)).to.be.true;

                dhh.updateReplicationConfig({
                    'source': SOURCE_DOMAIN,
                    'target': TARGET_DOMAIN,
                    'excludeExcept': defaultChannelName,
                    'historicalDays': 20
                }, function(res, body) {
                    expect(gu.isHTTPSuccess(res.status)).to.be.true;
                    gu.debugLog('Created default channel with name '+ defaultChannelName);

                    cb();
                })
            })

    }


    describe('Source-specific Replication config tests', function() {


        /*
            Ensure that:
            1) a defaultChannelName is chosen from SOURCE_DOMAIN
            2) a replication config exists on TARGET_DOMAIN for SOURCE_DOMAIN (if not, we create one)
         */
        before(function(done) {

            getDefaultChannel(function(selectedChannelName) {
                defaultChannelName = selectedChannelName;

                dhh.getReplicationConfig({'target': TARGET_DOMAIN, 'source': SOURCE_DOMAIN, 'debug': true},
                    function(res, body) {

                        if (gu.HTTPresponses.Not_Found == res.status) {
                            gu.debugLog('No config found for target '+ TARGET_DOMAIN +' and source '+ SOURCE_DOMAIN);

                            createBasicConfigEntry(done);
                        } else {
                            gu.debugLog('Config entry found.')
                        }

                        expect(res.status).to.equal(gu.HTTPresponses.OK);

                        gu.debugLog('Body: ');
                        console.log(body);


                        INITIAL_SOURCE_CONFIG = body;

                        done();
                    })
            })


        })

        it('Source config is legal format', function(done) {

            expect(INITIAL_SOURCE_CONFIG).to.not.be.null;

            expect(lodash.keys(INITIAL_SOURCE_CONFIG).length).to.equal(3);

            expect(INITIAL_SOURCE_CONFIG.hasOwnProperty('domain')).to.be.true;
            expect(INITIAL_SOURCE_CONFIG.hasOwnProperty('historicalDays')).to.be.true;
            expect(isNaN(INITIAL_SOURCE_CONFIG['historicalDays'])).to.be.false;

            expect(INITIAL_SOURCE_CONFIG.hasOwnProperty('excludeExcept')).to.be.true;

            done();
        })



    });

    describe('Full config (not source-specific) tests', function() {

        before(function(done) {
            dhh.getReplicationConfig({'target': TARGET_DOMAIN, 'debug': true},
                function(res, body) {

                    if (gu.HTTPresponses.Not_Found == res.status) {
                        gu.debugLog('No config found for target '+ TARGET_DOMAIN);

                        createBasicConfigEntry(done);

                    } else {
                        gu.debugLog('Config entry found.')
                    }

                    expect(res.status).to.equal(gu.HTTPresponses.OK);


                    gu.debugLog('Body: ');
                    console.log(body);


                    INITIAL_FULL_CONFIG = body;

                    done();
                })
        })

        it('Full config is legal format', function(done) {
            var body = INITIAL_FULL_CONFIG;

             expect(body.hasOwnProperty('domains')).to.be.true;
             expect(body.hasOwnProperty('status')).to.be.true;

             lodash.forEach(body['domains'], function(domain) {
                expect(lodash.keys(domain).length).to.equal(3);

                 lodash.forEach(['domain', 'historicalDays', 'excludeExcept'], function(domKey) {
                    expect(domain.hasOwnProperty(domKey));
                 })

                 expect(domain['domain']).to.not.be.null;
                 expect(domain['historicalDays']).to.not.be.null;
             })

             lodash.forEach(body['status'], function(status) {
             expect(lodash.keys(status).length).to.equal(6);

             lodash.forEach(['replicationLatest', 'sourceLatest', 'connected', 'deltaLatest', 'name', 'url'],
                function(statusKey) {
                    expect(status.hasOwnProperty(statusKey));
                })

             })

            done();

        })
    })


});