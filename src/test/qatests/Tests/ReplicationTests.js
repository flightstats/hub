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
    SOURCE_DOMAIN = 'gabetest.domain.fake',
    TARGET_DOMAIN = 'hub.svc.dev',
    DEBUG = true;


describe('Replication', function() {



    describe('Get Replication config', function() {

        // If target_domain's config is empty for source_domain, create an entry.
        before(function(done) {

            dhh.getReplicationConfig({'target': TARGET_DOMAIN, 'debug': true},
                function(res, body) {
                    expect(res.status).to.equal(gu.HTTPresponses.OK);

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


                    gu.debugLog('Body: ');
                    console.log(body);

//                    expect(body._links.hasOwnProperty('self')).to.be.true;

                done();
            })
        })

        it('has a config', function(done) {
            gu.debugLog('derp');
            done();
        })

    });


});