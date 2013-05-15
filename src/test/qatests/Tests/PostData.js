// POST DATA tests

var chai = require('chai');
var expect = chai.expect;
var superagent = require('superagent');
var request = require('request');
var moment = require('moment');
var async = require('async');
var crypto = require('crypto');
var fs = require('fs');

var dhh = require('.././DH_test_helpers/DHtesthelpers.js'),
    ranU = require('../randomUtils.js'),
    gu = require('../genericUtils.js');

var URL_ROOT = dhh.URL_ROOT,
    fakeChannelUri = [URL_ROOT, 'channel', dhh.makeRandomChannelName()].join('/');

// The following paths assume this is being run from a parent directory. The before() method will adjust this if
//  the test is being run in this file's directory
var CAT_TOILET_PIC = './artifacts/cattoilet.jpg',
    MY_2MB_FILE = './artifacts/Iam2_5Mb.txt',
    MY_2KB_FILE = './artifacts/Iam200kb.txt';


// Test variables that are regularly overwritten
var agent
    , payload
    , fileAsAStream
    , req
    , uri
    , contentType;

var channelName,
    channelUri,
    DEBUG = true;




describe('POST data to channel:', function(){

    before(function(done){

        // Update file paths if test is run in its own directory.
        var cwd = process.cwd();
        var dirRegex = /\/([^/]+)$/;
        var parent = cwd.match(dirRegex)[1];
        if ('postdata'.toLowerCase() == parent.toLowerCase()) {
            CAT_TOILET_PIC = '../'+ CAT_TOILET_PIC;
            MY_2KB_FILE = '../'+ MY_2KB_FILE;
            MY_2MB_FILE = '../'+ MY_2MB_FILE;
        };

        channelName = dhh.makeRandomChannelName();
        agent = superagent.agent();
        dhh.makeChannel(channelName, function(res, cnUri){
            if ((res.error) || (!gu.isHTTPSuccess(res.status))) {
                done(res.error);
            };
            channelUri = cnUri;
            gu.debugLog('Main test channel:'+ channelName);

            done();
        });
    });

    beforeEach(function(){
        agent = superagent.agent();
        payload = uri = req = contentType = '';
    })

    it('Acceptance - should return a (DATA_POST_SUCCESS) for POSTing data', function(done){

        payload = ranU.randomString(Math.round(Math.random() * 50));

        dhh.postData(channelUri, payload, function(res, uri) {
            gu.debugLog('Response from POST attempt: '+ res.status, DEBUG);
            expect(gu.isHTTPSuccess(res.status)).to.equal(true);

            dhh.getValidationString(uri, payload, done);
        });

    });


    // Fails with in-memory backend:  https://www.pivotaltracker.com/story/show/49567193
    it('POST should return a 404 trying to save to nonexistent channel', function(done){
        dhh.postData(fakeChannelUri, dhh.getRandomPayload(), function(res, uri) {
            expect(res.status).to.equal(gu.HTTPresponses.Not_Found);

            done();
        });
    });


    it('POST same set of data twice to channel', function(done){
        payload = ranU.randomString(Math.round(Math.random() * 50));

        dhh.postData(channelUri, payload, function(res, uri) {
            expect(gu.isHTTPSuccess(res.status)).to.equal(true);

            dhh.postData(channelUri, payload, function(res2, uri2) {
                expect(gu.isHTTPSuccess(res2.status)).to.equal(true);

                dhh.getValidationString(uri, payload, done);
            });
        });
    });

    it('POST same set of data to two different channels', function(done) {
        var otherChannelName = dhh.makeRandomChannelName(),
            otherChannelUri;

        dhh.makeChannel(otherChannelName, function(res, cnUri) {
            otherChannelUri = cnUri;
            expect(gu.isHTTPSuccess(res.status)).to.equal(true);
            payload = ranU.randomString(Math.round(Math.random() * 50));

            dhh.postData(channelUri, payload, function(res, uri) {
                expect(gu.isHTTPSuccess(res.status)).to.equal(true);

                dhh.getValidationString(uri, payload, function() {

                    dhh.postData(otherChannelUri, payload, function(res2, uri2) {
                        expect(gu.isHTTPSuccess(res2.status)).to.equal(true);

                        dhh.getValidationString(uri2, payload, done);
                    });

                });
            });

        });
    });


    it('POST empty data set to channel', function(done){
        dhh.postData(channelUri, '', function(res, uri) {
            expect(gu.isHTTPSuccess(res.status)).to.equal(true);

            dhh.getValidationString(uri, '', done);
        });

    });

    it('POST 200kb file to channel', function(done) {
        payload = fs.readFileSync(MY_2KB_FILE, "utf8");

        dhh.postData(channelUri, payload, function(res, uri) {
            expect(gu.isHTTPSuccess(res.status)).to.equal(true);

            dhh.getValidationString(uri, payload, done);
        });
    });


    it('POST 1,000 characters to channel', function(done) {
        payload = ranU.randomString(1000, ranU.simulatedTextChar);

        dhh.postData(channelUri, payload, function(res, uri) {
            expect(gu.isHTTPSuccess(res.status)).to.equal(true);

            dhh.getValidationString(uri, payload, done);
        });
    });

    // Confirms via md5 checksum
    it('POST image file to channel and recover', function(done) {
        var fileAsAStream = fs.createReadStream(CAT_TOILET_PIC);

        fileAsAStream.pipe(request.post(channelUri,
            function(err, res, body) {
                if (err) {
                    throw err;
                }
                expect(gu.isHTTPSuccess(res.statusCode)).to.equal(true);
                var cnMetadata = new dhh.channelMetadata(JSON.parse(body));
                uri = cnMetadata.getChannelUri();

                var md5sum = crypto.createHash('md5'),
                    s = fs.ReadStream(CAT_TOILET_PIC);

                s.on('data', function(d) {
                    md5sum.update(d);
                }).on('end', function() {
                        var expCheckSum = md5sum.digest('hex');

                        dhh.getValidationChecksum(uri, expCheckSum, done);
                    });

            })
        );

    });


    // For story:  Provide the client with a creation-timestamp in the response from a data storage request.k
    // https://www.pivotaltracker.com/story/show/43221779

    describe('POST - Creation timestamps returned:', function() {

        it('Creation timestamp returned on data storage', function(done){

            var timestamp = '',
                payload = ranU.randomString(Math.round(Math.random() * 50));

            dhh.postData(channelUri, payload, function(res, uri) {
                expect(gu.isHTTPSuccess(res.status)).to.equal(true);
                timestamp = moment(res.body.timestamp);

                expect(moment(timestamp).isValid()).to.be.true;
                done();
            });
        });

        it('Multiple POSTings of data to a channel should return ever-increasing creation timestamps.', function(done) {
            var respMoment;

            // will be set to the diff between now and initial response time plus five minutes, just to ensure there
            //      aren't any egregious shenanigans afoot. In milliseconds.
            var serverTimeDiff;

            async.waterfall([
                function(callback){
                    setTimeout(function(){
                        payload = ranU.randomString(ranU.randomNum(51));

                        dhh.postData(channelUri, payload, function(res, uri) {
                            expect(gu.isHTTPSuccess(res.status)).to.equal(true);
                            respMoment = moment(res.body.timestamp);
                            //gu.debugLog('Creation time was: '+ respMoment.format('X'));

                            expect(respMoment.isValid()).to.be.true;
                            serverTimeDiff = moment().diff(respMoment) + 300000;

                            callback(null, respMoment);
                        });
                    }, 1000);
                }
                ,function(lastResp, callback){
                    setTimeout(function(){
                        payload = ranU.randomString(ranU.randomNum(51));

                        dhh.postData(channelUri, payload, function(res, uri) {
                            expect(gu.isHTTPSuccess(res.status)).to.equal(true);
                            respMoment = moment(res.body.timestamp);
                            //gu.debugLog('Creation time was: '+ respMoment.format('X'));

                            expect(respMoment.isValid()).to.be.true;
                            expect(respMoment.isAfter(lastResp)).to.be.true;
                            expect(moment().diff(respMoment)).to.be.at.most(serverTimeDiff);

                            callback(null, respMoment);
                        });
                    }, 1000);

                }
                ,function(lastResp, callback){
                    setTimeout(function(){

                        payload = ranU.randomString(ranU.randomNum(51));

                        dhh.postData(channelUri, payload, function(res, uri) {
                            expect(gu.isHTTPSuccess(res.status)).to.equal(true);
                            respMoment = moment(res.body.timestamp);
                            //gu.debugLog('Creation time was: '+ respMoment.format('X'));

                            expect(respMoment.isValid()).to.be.true;
                            expect(respMoment.isAfter(lastResp)).to.be.true;
                            expect(moment().diff(respMoment)).to.be.at.most(serverTimeDiff);

                            callback(null);
                        });
                    }, 1000);
                }
            ]
                ,function(err) {
                    if (err) throw err;
                    done();
                });
        });


        // TODO: POST data from different timezone and confirm timestamp is correct?
    });

    // For story: Provide "self" URI in the Location Header upon storing data  https://www.pivotaltracker.com/story/show/44845167
    describe('POST, Location header in response:', function() {
        it('Acceptance - location header exists and is correct', function(done) {
            payload = ranU.randomString(Math.round(Math.random() * 50));

            dhh.postData(channelUri, payload, function(res, uri) {
                expect(gu.isHTTPSuccess(res.status)).to.equal(true);
                var myHeader = new dhh.packetPOSTHeader(res.headers),
                    location = myHeader.getLocation();
                expect(location).to.equal(uri);

                done();
            });
        });

        // Fails with in-memory backend due to https://www.pivotaltracker.com/story/show/49567193
        it('Negative - failed attempt has no location header:', function(done) {
            payload = ranU.randomString(Math.round(Math.random() * 50));

            dhh.postData(fakeChannelUri, payload, function(res, uri) {
                expect(gu.isHTTPError(res.status)).to.equal(true);
                var myHeader = new dhh.packetPOSTHeader(res.headers),
                    location = myHeader.getLocation();
                expect(location).to.be.null;

                done();
            });
        });
    });

});