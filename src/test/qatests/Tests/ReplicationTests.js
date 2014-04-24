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

    var INITIAL_SOURCE_CONFIG = null;      // The rep config for SOURCE_DOMAIN

    describe('Get Replication config', function() {

        var defaultChannelName = dhh.getRandomChannelName();

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

        // If target_domain's config is empty for source_domain, create an entry.
        before(function(done) {

            dhh.getReplicationConfig({'target': TARGET_DOMAIN, 'source': SOURCE_DOMAIN, 'debug': true},
                function(res, body) {

                    if (gu.HTTPresponses.Not_Found == res.status) {
                        gu.debugLog('No config found for target '+ TARGET_DOMAIN +' and source '+ SOURCE_DOMAIN);

                        createBasicConfigEntry(done);
                    } else {
                        gu.debugLog('Config entry found.')
                    }

                    expect(res.status).to.equal(gu.HTTPresponses.OK);

                    // This is for getting config without specifying source:
                    /*

                    expect(body.hasOwnProperty('domains')).to.be.true;
                    expect(body.hasOwnProperty('status')).to.be.true;

                    lodash.forEach(body['domains'], function(domain) {
                        expect(lodash.keys(domain).length).to.equal(4);

                        lodash.forEach(['domain', 'historicalDays', 'includeExcept', 'excludeExcept'], function(domKey) {
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
                    */

                    gu.debugLog('Body: ');
                    console.log(body);

//                    expect(body._links.hasOwnProperty('self')).to.be.true;

                    INITIAL_SOURCE_CONFIG = body;

                done();
            })

        })

        it('has a legal config', function(done) {

            expect(INITIAL_SOURCE_CONFIG).to.not.be.null;

            expect(lodash.keys(INITIAL_SOURCE_CONFIG).length).to.equal(4);

            expect(INITIAL_SOURCE_CONFIG.hasOwnProperty('domain')).to.be.true;
            expect(INITIAL_SOURCE_CONFIG.hasOwnProperty('historicalDays')).to.be.true;
            expect(isNaN(INITIAL_SOURCE_CONFIG['historicalDays'])).to.be.false;

            expect(INITIAL_SOURCE_CONFIG.hasOwnProperty('includeExcept')).to.be.true;
            expect(INITIAL_SOURCE_CONFIG.hasOwnProperty('excludeExcept')).to.be.true;

            // Either the exclude or include list must be empty, but not both.
            var incl = INITIAL_SOURCE_CONFIG['includeExcept'],
                excl = INITIAL_SOURCE_CONFIG['excludeExcept'];

            if (incl.length > 0) {
                expect(excl.length).to.equal(0);
            }
            else {
                expect(incl.length).to.equal(0);
            }


            done();
        })

    });


});