/**
 * Created with JetBrains WebStorm.
 * User: gnewcomb
 * Date: 3/1/13
 * Time: 9:47 AM
 * To change this template use File | Settings | File Templates.
 */

// Load tests

var chai = require('chai');
var expect = chai.expect;
var superagent = require('superagent');
var crypto = require('crypto');
var request = require('request');
var moment = require('moment');
var async = require('async');
var fs = require('fs');

var dhh = require('.././DH_test_helpers/DHtesthelpers.js'),
    ranU = require('../randomUtils.js'),
    gu = require('../genericUtils.js');

var MY_4MB_FILE = './artifacts/Iam4Mb.txt';
var MY_8MB_FILE = './artifacts/Iam8Mb.txt';
var MY_16MB_FILE = './artifacts/Iam16Mb.txt';
var MY_32MB_FILE = './artifacts/Iam32Mb.txt';
var MY_64MB_FILE = './artifacts/Iam64Mb.txt';

var URL_ROOT = dhh.URL_ROOT;

// Test variables that are regularly overwritten
var agent
    , payload
    , fileAsAStream
    , req
    , uri
    , contentType;

var channelName,
    channelUri;

describe.skip('Load tests - POST data:', function(){

    var loadChannels = {},
        loadChannelKeys = [];  // channel.uri (to fetch data) and channel.data, e.g. { con {uri: x, data: y}}

    var postAndConfirmBigFile = function(fileLocation, callback) {
        payload = fs.readFileSync(fileLocation, "utf8");

        dhh.postData(channelUri, payload, function(res, uri) {
            expect(gu.isHTTPSuccess(res.status)).to.equal(true);

            dhh.confirmExpectedData(uri, payload, function(didMatch) {
                expect(didMatch).to.be.true;

                callback();
            });
        });
    }

    before(function(myCallback){
        channelName = dhh.getRandomChannelName();
        agent = superagent.agent();
        dhh.createChannel(channelName, function(res, cnUri){
            if ((res.error) || (!gu.isHTTPSuccess(res.status))) {
                myCallback(res.error);
            };

            channelUri = cnUri;
            gu.debugLog('Main test channel:'+ channelName);

            myCallback();
        });
    });

    beforeEach(function(){
        agent = superagent.agent();
        payload = uri = req = contentType = '';
    })

    describe('Rapid data posting:', function() {
        // To ignore the Loadtest cases:  mocha -R nyan --timeout 4000 --grep Load --invert

        it('Loadtest - POST rapidly to ten different channels, then confirm data retrieved via GET is correct', function(done){
            var VERBOSE = true,
                cnMetadata,
                numIterations = 20;

            this.timeout(5000 * numIterations);
            for (var i = 1; i <= numIterations; i++)
            {
                var thisName = dhh.getRandomChannelName(),
                    thisChannelUrl,
                    thisPayload = ranU.randomString(Math.round(Math.random() * 50));
                loadChannels[thisName] = {"channelUri": null, "uri":'', "data":thisPayload};

            }

            for (var x in loadChannels){
                if (loadChannels.hasOwnProperty(x)) {
                    loadChannelKeys.push(x);
                }
            }

            async.each(loadChannelKeys, function(cn, callback) {
                dhh.createChannel(cn, function(res, cnUri) {
                    loadChannels[cn]['channelUri'] = cnUri;
                    expect(gu.isHTTPSuccess(res.status)).to.equal(true);
                    cnMetadata = new dhh.channelMetadata(res.body);
                    expect(cnMetadata.getChannelUri()).to.equal(URL_ROOT +'/channel/'+ cn);
                    callback();
                });

            }, function(err) {
                if (err) {
                    throw err;
                };

                async.each(loadChannelKeys, function(cn, callback) {

                    dhh.postData(loadChannels.channelUri,loadChannels[cn].data, function(res, uri) {
                        loadChannels[cn].uri = uri;
                        callback();
                    });

                }, function(err) {
                    if (err) {
                        throw err;
                    };

                    async.eachSeries(loadChannelKeys, function(cn, callback) {
                        uri = loadChannels[cn].uri;
                        payload = loadChannels[cn].data;

                        dhh.confirmExpectedData(uri, payload, function(didMatch){
                            //gu.debugLog('Confirmed data retrieval from channel: '+ cn);
                            expect(didMatch).to.be.true;

                            callback();
                        });
                    }, function(err) {
                        if (err) {
                            throw err;
                        };

                        done();
                    });

                });

            });

        });
    });

    describe.skip('Post big files:', function() {

        it('POST 2 MB file to channel', function(done) {
            postAndConfirmBigFile(MY_2MB_FILE, done);
        });

        it('POST 4 MB file to channel', function(done) {
            this.timeout(60000);

            postAndConfirmBigFile(MY_4MB_FILE, done);
        });

        it('POST 8 MB file to channel', function(done) {
            this.timeout(120000);

            postAndConfirmBigFile(MY_8MB_FILE, done);
        });

        it('POST 16 MB file to channel', function(done) {
            this.timeout(240000);

            postAndConfirmBigFile(MY_16MB_FILE, done);
        });
    });

    describe.skip('Unsupported scenarios (tests ignored):', function() {
        // as of 3/5/2012, DH cannot handle files this big
        it.skip('<UNSUPPORTED> POST and retrieve 32 MB file to channel', function(done) {
            this.timeout(480000);

            postAndConfirmBigFile(MY_32MB_FILE, done);
        });

        // as of 3/5/2012, DH cannot handle files this big
        it.skip('<UNSUPPORTED> POST and retrieve 64 MB file to channel', function(done) {
            this.timeout(960000);

            postAndConfirmBigFile(MY_64MB_FILE, done);
        });
    });
});