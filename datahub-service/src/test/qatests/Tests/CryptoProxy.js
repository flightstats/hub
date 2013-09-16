/**
 * Created with IntelliJ IDEA.
 * User: gnewcomb
 * Date: 9/13/13
 * Time: 3:59 PM
 * To change this template use File | Settings | File Templates.
 */

// POST DATA tests

var chai = require('chai');
var expect = chai.expect;
var superagent = require('superagent'),
    moment = require('moment'),
    async = require('async'),
    lodash = require('lodash'),
    url = require('url');

var dhh = require('.././DH_test_helpers/DHtesthelpers.js'),
    ranU = require('../randomUtils.js'),
    gu = require('../genericUtils.js');

var URL_ROOT = dhh.URL_ROOT,
    DOMAIN = '10.11.15.162:8081';
    //DOMAIN = 'datahub.svc.dev';

describe.skip('CODE NOT DEPLOYED YET - Crypto Proxy testing', function() {

    describe('Proxy functionality', function() {
        var cnName,
            cnDirectUri,
            cnProxyUri;

        before(function(done) {
            cnName = dhh.getRandomChannelName();
            dhh.createChannel({name: cnName}, function(res, uri){
                if (res.error){
                    gu.debugLog('Error in createChannel: '+ res.error);
                    done();
                }
                else if (!gu.isHTTPSuccess(res.status)) {
                    gu.debugLog()
                    done();
                }
                else {
                    cnDirectUri = uri;
                    var parsed = url.parse(cnDirectUri);
                    cnProxyUri = 'http://'+ DOMAIN + parsed.path;
                    gu.debugLog('Main test channel direct URI:'+ cnDirectUri);
                    gu.debugLog('Main test channel via Proxy: '+ cnProxyUri);

                    done();
                }
            });
        })

        describe('Acceptance', function() {
            var payload = dhh.getRandomPayload(),
                dataDirectUri,
                dataProxyUri,
                postResponse,
                getResponse,// must check .statusCode, not .status, since this uses HTTP instead of SuperAgent
                getData;

            // Post and Get data from channel both via CP
            before(function(done) {
                dhh.postData({channelUri: cnProxyUri, data: payload, debug: true}, function(res, dataUri) {
                    if (gu.isHTTPSuccess(res.status)) {
                        postResponse = res;
                        dataDirectUri = dataUri;
                        var parsed = url.parse(dataUri);
                        dataProxyUri = 'http://'+ DOMAIN + parsed.path;
                        gu.debugLog('data uri via proxy: '+ dataProxyUri);

                        dhh.getDataFromChannel({uri: dataProxyUri, debug: true}, function(err, getRes, gottenData) {
                            if (gu.isHTTPSuccess(getRes.status)) {
                                getResponse = getRes;
                                getData = gottenData;

                                done();
                            }
                            else {  // GET failed
                                done();
                            }
                        })
                    }
                    else {  // POST failed
                        done();
                    }

                })

            })

            it('Insert data via Crypto Proxy', function() {

                it('Returned 201', function() {
                    expect(postResponse.status).to.equal(gu.HTTPresponses.Created);
                })

                it('body has correct structure', function() {
                    var body = postResponse.body;

                    expect(body.hasOwnProperty('_links')).to.be.true;
                    expect(body._links.hasOwnProperty('channel')).to.be.true;
                    expect(body._links.hasOwnProperty('self')).to.be.true;
                    expect(body._links.channel.hasOwnProperty('href')).to.be.true;
                    expect(body._links.self.hasOwnProperty('href')).to.be.true;
                    expect(body.hasOwnProperty('timestamp')).to.be.true;

                    expect(lodash.keys(body).length).to.equal(2);
                    expect(lodash.keys(body._links).length).to.equal(2);
                })

                it('data URI correctly uses direct datahub URI, not CP', function() {
                    expect(postResponse.body._links.self.toString().lastIndexOf(DOMAIN)).to.equal(-1);
                    expect(postResponse.body._links.self.toString().lastIndexOf(URL_ROOT)).to.be.at.least(0);
                })

                it('channel link is correct', function() {
                    expect(postResponse.body._links.channel.href).to.equal(cnDirectUri);
                })

                it('timestamp is correct', function() {
                    var theTimestamp = moment(postResponse.body.timestamp);

                    expect(theTimestamp.add('minutes', 5).isAfter(moment())).to.be.true;
                })
            })

            it('Get data via Crypto Proxy', function() {

                it('Returned 200', function() {
                    expect(getResponse.statusCode).to.equal(gu.HTTPresponses.OK);
                })

                // Item was inserted via CP, so should match on retrieval
                it('Returned the correct payload', function() {
                    expect(payload).to.equal(getData);
                })
            })
        })

        describe('Error cases', function() {

            describe('Other calls to CP return error', function() {
                var cnBody;

                before(function(done) {
                    dhh.getChannel({uri: cnDirectUri}, function(res, body) {
                        if (gu.isHTTPSuccess(res.status)) {
                            cnBody = body;
                        }

                        done();
                    })
                })

                var doDebug = true;

                it('List all channels returns client error', function(done) {
                    dhh.getAllChannels({domain: DOMAIN, debug: doDebug}, function(res, channels) {
                        expect(gu.isHTTPClientError(res.status)).to.be.true;
                        done();
                    })
                })

                it('Create channel returns client error', function(done) {
                    dhh.createChannel({name: dhh.getRandomChannelName(), domain: DOMAIN, debug: doDebug}, function(res, cnUri) {
                        expect(gu.isHTTPClientError(res.status)).to.be.true;
                        done();
                    })
                })

                it('Update channel returns client error', function(done) {
                    dhh.patchChannel({channelUri: cnProxyUri, ttlMillis: 51515, debug: doDebug}, function(res) {
                        expect(gu.isHTTPClientError(res.status)).to.be.true;
                        done();
                    })
                })

                it('Get channel returns client error', function(done) {
                    dhh.getChannel({uri: cnProxyUri, debug: doDebug}, function(res, body) {
                        expect(gu.isHTTPClientError(res.status)).to.be.true;
                        done();
                    })
                })

                it('Get latest from channel returns client error', function(done) {
                    // Get channel directly (not via CP) for latest link
                    var cnMetadata = new dhh.channelMetadata(cnBody),
                        latestUri = cnMetadata.getLatestUri();

                    superagent.agent().get(latestUri)
                        .end(function(err, latestRes) {
                            if (err) {
                                throw err
                            };

                            expect(gu.isHTTPClientError(latestRes.status)).to.be.true;

                            done();
                        });


                })

                it('Subscribing to channel returns client error', function(done) {
                    var socket,
                        cnMetadata = new dhh.channelMetadata(cnBody),
                        parsed = url.parse(cnMetadata.getWebSocketUri()),
                        wsUri = 'ws://'+ DOMAIN + parsed.path;

                    gu.debugLog('Opening socket to '+ wsUri);

                    // if this function is called, then we didn't get an error
                    var onOpen = function() {
                        expect(true).to.be.false;
                        done();
                    }

                    var onError = function(msg) {
                        expect(msg.toString().lastIndexOf('405')).to.be.at.least(0);

                        done();
                    }

                    socket = new dhh.WSWrapper({
                        'domain': DOMAIN,
                        'uri': wsUri,
                        'socketName': 'ws_01',
                        'onOpenCB': onOpen,
                        'onMessageCB': null,
                        'onErrorCB': onError
                    });
                    socket.createSocket();
                })

            })
        })

        describe('Scenarios', function() {

            it('Item created through CP can be fetched directly from DH', function(done) {
                var payload = dhh.getRandomPayload();

                dhh.postData({channelUri: cnProxyUri, data: payload}, function(res, dataUri) {
                    expect(res.status).to.equal(gu.HTTPresponses.Created);

                    dhh.getDataFromChannel({uri: dataUri}, function(err, res, data) {
                        expect(err).to.be.null;
                        expect(res.statusCode).to.equal(gu.HTTPresponses.OK);

                        // The data on retrieval should NOT match what was inserted, as it was encrypted on insertion,
                        //      but not on retrieval.
//                        expect(data).to.equal(payload);

                        done();
                    })
                })
            })

            it('Item created directly in DH can be fetched through CP', function(done) {
                var payload = dhh.getRandomPayload();

                dhh.postData({channelUri: cnDirectUri, data: payload}, function(res, dataUri) {
                    expect(res.status).to.equal(gu.HTTPresponses.Created);

                    var parsed = url.parse(dataUri),
                        directUri = 'http://'+ DOMAIN + parsed.path;

                    dhh.getDataFromChannel({uri: directUri}, function(err, res, data) {
                        expect(err).to.be.null;
                        expect(res.statusCode).to.equal(gu.HTTPresponses.OK);

                        // The data on retrieval should NOT match what was inserted, as it was encrypted on retrieval,
                        //      but not on insertion.
//                        expect(data).to.equal(payload);

                        done();
                    })
                })
            })
        })
    })
})


