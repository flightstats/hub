// CREATE CHANNEL tests

var chai = require('chai');
var expect = chai.expect;
var superagent = require('superagent');
var async = require('async');
var _ = require('lodash');

var dhh = require('.././DH_test_helpers/DHtesthelpers.js');
var ranU = require('../randomUtils.js');
var gu = require('../genericUtils.js');

var URL_ROOT = dhh.URL_ROOT;

// Test variables that are regularly overwritten
var agent
    , payload
    , req
    , uri
    , contentType;

var channelName,
    channelUri;

// used for a few tests that require a series of items in a channel -- (only in one test so far :P )
var firstValueUri, secondValueUri, thirdValueUri;

describe('HEAD on data tests:', function() {

    before(function(myCallback){
        channelName = dhh.getRandomChannelName();
        agent = superagent.agent();
        dhh.createChannel({name: channelName}, function(res, cnUri){
            if ((res.error) || (!gu.isHTTPSuccess(res.status))) {
                gu.debugLog('\nDump!');
                console.dir(res);

                myCallback(res.error);
            };

            channelUri = cnUri;
            console.log('Main test channel:'+ channelName);

            async.series([
                function(callback){
                    dhh.postData({channelUri: channelUri, data: dhh.getRandomPayload()}, function(res, myUri) {
                        expect(gu.isHTTPSuccess(res.status)).to.equal(true);
                        firstValueUri = myUri;
                        callback(null,null);
                    });
                },
                function(callback){
                    dhh.postData({channelUri: channelUri, data: dhh.getRandomPayload()}, function(res, myUri) {
                        expect(gu.isHTTPSuccess(res.status)).to.equal(true);
                        secondValueUri = myUri;
                        callback(null,null);
                    });
                },
                function(callback){
                    dhh.postData({channelUri: channelUri, data: dhh.getRandomPayload()}, function(res, myUri) {
                        expect(gu.isHTTPSuccess(res.status)).to.equal(true);
                        thirdValueUri = myUri;

                        callback(null, null);
                    });
                }
            ],
                function(err, results){
                    myCallback();
                });
        });
    });

    beforeEach(function(){
        agent = superagent.agent();
        payload = uri = req = contentType = '';
    });

    it('Acceptance: HEAD returns all expected headers for item with siblings in a channel', function(done) {
        var getHeaders,
            headHeaders;

        superagent.agent().get(secondValueUri)
            .end(function(err, res) {
                getHeaders = res.headers;

                superagent.agent().head(secondValueUri)
                    .end(function(err2, res2) {
                        headHeaders = res2.headers;

                        expect(gu.dictCompare(getHeaders, headHeaders)).to.be.true;

                        done();
                    });
            });
    });

    it('HEAD does not return a message body', function(done) {
        superagent.agent().head(secondValueUri)
            .end(function(err, res) {
                expect(_.size(res.body)).to.equal(0);
                done();
            });
    });

    it('HEAD on a fake item URI returns 404', function(done) {
        var iSlash = secondValueUri.lastIndexOf('/');
        uri = secondValueUri.substring(0,iSlash) + '/0'+ secondValueUri.substring(iSlash + 1);

        superagent.agent().head(uri)
            .end(function(err, res) {
                expect(gu.isHTTPError(res.status)).to.equal(true);
                done();
            });
    });

})
