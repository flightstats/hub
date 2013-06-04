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
var async = require('async'),
    lodash = require('lodash');

var dhh = require('.././DH_test_helpers/DHtesthelpers.js'),
    ranU = require('../randomUtils.js'),
    gu = require('../genericUtils.js');

var MY_4MB_FILE = './artifacts/Iam4Mb.txt',
    MY_8MB_FILE = './artifacts/Iam8Mb.txt',
    MY_16MB_FILE = './artifacts/Iam16Mb.txt',
    MY_32MB_FILE = './artifacts/Iam32Mb.txt',
    MY_64MB_FILE = './artifacts/Iam64Mb.txt';

var URL_ROOT = dhh.URL_ROOT;

var channelName,
    channelUri;

describe('Load tests - POST data:', function(){

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

        dhh.createChannel(channelName, function(res, cnUri){
            if ((res.error) || (!gu.isHTTPSuccess(res.status))) {
                myCallback(res.error);
            };

            channelUri = cnUri;
            gu.debugLog('Main test channel:'+ channelName);

            myCallback();
        });
    });

    describe('Rapid data posting:', function() {
        // To ignore the Loadtest cases:  mocha -R nyan --timeout 4000 --grep Load --invert

        it('Loadtest - POST rapidly to many different channels, then confirm data retrieved via GET is correct', function(done){
            var cnMetadata,
                numIterations = 30,
                VERBOSE = true;

            this.timeout(5000 * numIterations);

            for (var i = 1; i <= numIterations; i++)
            {
                var thisName = dhh.getRandomChannelName(),
                    thisPayload = ranU.randomString(Math.round(Math.random() * 50));

                loadChannels[thisName] = {
                    channelUri: null,
                    dataUri: null,
                    data: thisPayload
                };

            }

            loadChannelKeys = lodash.keys(loadChannels);

            async.each(loadChannelKeys,
                function(cnName, cb) {

                    dhh.createChannel(cnName, function(createRes, cnUri) {
                        loadChannels[cnName].channelUri = cnUri;
                        expect(createRes.status).to.equal(gu.HTTPresponses.Created);
                        gu.debugLog('CREATED new channel at: '+ cnUri, VERBOSE);

                        dhh.postData(cnUri, loadChannels[cnName].data, function(postRes, theDataUri) {
                            loadChannels[cnName].dataUri = theDataUri;
                            gu.debugLog('INSERTED data at: '+ theDataUri, VERBOSE);

                            dhh.confirmExpectedData(theDataUri, loadChannels[cnName].data, function(didMatch){
                                expect(didMatch).to.be.true;
                                gu.debugLog('CONFIRMED data with GET at: '+ theDataUri, VERBOSE);

                                cb(null);
                            })
                        })
                    })
                },
                function(err) {

                    done();
                }
            );

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