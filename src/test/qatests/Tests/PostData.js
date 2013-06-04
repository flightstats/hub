// POST DATA tests

var chai = require('chai');
var expect = chai.expect;
var superagent = require('superagent');
var request = require('request');
var moment = require('moment');
var async = require('async'),
    crypto = require('crypto'),
    fs = require('fs'),
    lodash = require('lodash');

var dhh = require('.././DH_test_helpers/DHtesthelpers.js'),
    ranU = require('../randomUtils.js'),
    gu = require('../genericUtils.js');

var URL_ROOT = dhh.URL_ROOT,
    fakeChannelUri = [URL_ROOT, 'channel', dhh.getRandomChannelName()].join('/');

// The following paths assume this is being run from a parent directory. The before() method will adjust this if
//  the test is being run in this file's directory
var CAT_TOILET_PIC = './artifacts/cattoilet.jpg',
    MY_2MB_FILE = './artifacts/Iam2_5Mb.txt',
    MY_2KB_FILE = './artifacts/Iam200kb.txt';


var channelName,
    channelUri,
    DEBUG = true;




describe('POST data to channel:', function(){

    var randomPayload;

    var postAndConfirmData = function(data, callback) {
        dhh.postData(channelUri, data, function(res, uri) {
            expect(gu.isHTTPSuccess(res.status)).to.equal(true);

            dhh.confirmExpectedData(uri, data, function(didMatch) {
                expect(didMatch).to.be.true;

                callback();
            });
        });
    }

    before(function(done){

        // Update file paths if test is run in its own directory.
        var cwd = process.cwd(),
            dirRegex = /\/([^/]+)$/,
            parent = cwd.match(dirRegex)[1];

        if ('postdata'.toLowerCase() == parent.toLowerCase()) {
            CAT_TOILET_PIC = '../'+ CAT_TOILET_PIC;
            MY_2KB_FILE = '../'+ MY_2KB_FILE;
            MY_2MB_FILE = '../'+ MY_2MB_FILE;
        };

        channelName = dhh.getRandomChannelName();
        dhh.createChannel(channelName, function(res, cnUri){
            if ((res.error) || (!gu.isHTTPSuccess(res.status))) {
                done(res.error);
            };
            channelUri = cnUri;
            gu.debugLog('Main test channel:'+ channelName);

            done();
        });
    });

    beforeEach(function() {
        randomPayload = dhh.getRandomPayload();
    })

    describe('Acceptance', function() {
        var mainPayload,
            mainResponse,
            mainDataUri;

        before(function(done) {

            mainPayload = randomPayload;

            dhh.postData(channelUri, mainPayload, function(res, dataUri) {
                mainResponse = res;
                mainDataUri = dataUri;

                done();
            });
        })

        it('returns 201 (Created)', function(){
            expect(mainResponse.status).to.equal(gu.HTTPresponses.Created);
        });

        it('body has correct structure', function() {
            var body = mainResponse.body;

            expect(body.hasOwnProperty('_links')).to.be.true;
            expect(body._links.hasOwnProperty('channel')).to.be.true;
            expect(body._links.hasOwnProperty('self')).to.be.true;
            expect(body._links.channel.hasOwnProperty('href')).to.be.true;
            expect(body._links.self.hasOwnProperty('href')).to.be.true;
            expect(body.hasOwnProperty('timestamp')).to.be.true;

            expect(lodash.keys(body).length).to.equal(2);
            expect(lodash.keys(body._links).length).to.equal(2);
        })

        it('channel link is correct', function() {
            expect(mainResponse.body._links.channel.href).to.equal(channelUri);
        })

        it('timestamp is correct', function() {
            var theTimestamp = moment(mainResponse.body.timestamp);

            expect(theTimestamp.add('minutes', 5).isAfter(moment())).to.be.true;
        })

        it('data link is correct and data was successfully saved (confirm with get)', function(done) {

            dhh.confirmExpectedData(mainDataUri, mainPayload, function(didMatch) {
                expect(didMatch).to.be.true;

                done();
            });
        })

        it('POST should return a 404 trying to save to nonexistent channel', function(done){

            dhh.postData(fakeChannelUri, randomPayload, function(res, uri) {
                expect(res.status).to.equal(gu.HTTPresponses.Not_Found);

                done();
            });
        });
    })

    describe('Scenarios', function() {

        it('POST same set of data twice to channel', function(done){

            dhh.postData(channelUri, randomPayload, function(res, uri) {
                expect(gu.isHTTPSuccess(res.status)).to.equal(true);

                dhh.postData(channelUri, randomPayload, function(res2, uri2) {
                    expect(gu.isHTTPSuccess(res2.status)).to.equal(true);

                    dhh.confirmExpectedData(uri, randomPayload, function(didMatch) {
                        expect(didMatch).to.be.true;

                        done();
                    });
                });
            });
        });

        it('POST same set of data to two different channels', function(done) {
            var otherChannelName = dhh.getRandomChannelName(),
                otherChannelUri;

            dhh.createChannel(otherChannelName, function(res, cnUri) {
                otherChannelUri = cnUri;
                expect(gu.isHTTPSuccess(res.status)).to.equal(true);

                dhh.postData(channelUri, randomPayload, function(res, uri) {
                    expect(gu.isHTTPSuccess(res.status)).to.equal(true);

                    dhh.confirmExpectedData(uri, randomPayload, function(didMatchFirst) {
                        expect(didMatchFirst).to.be.true;

                        dhh.postData(otherChannelUri, randomPayload, function(res2, uri2) {
                            expect(gu.isHTTPSuccess(res2.status)).to.equal(true);

                            dhh.confirmExpectedData(uri2, randomPayload, function(didMatchSecond) {
                                expect(didMatchSecond).to.be.true;

                                done();
                            });
                        });

                    });
                });

            });
        });

        it('POST empty data set to channel', function(done){
            postAndConfirmData('', done);
        });

        it('POST 200kb file to channel', function(done) {
            var payload = fs.readFileSync(MY_2KB_FILE, "utf8");

            postAndConfirmData(payload, done);
        });

        it('POST 1,000 characters to channel', function(done) {
            var payload = ranU.randomString(1000, ranU.simulatedTextChar);

            postAndConfirmData(payload, done);
        });

        // Confirms via md5 checksum
        // As of June 3, 2013, this thing is timing out on the GET intermittently.
        it('POST image file to channel and recover', function(done) {

            var fileAsAStream = fs.createReadStream(CAT_TOILET_PIC),
                VERBOSE = true;

            fileAsAStream.pipe(request.post(channelUri,
                function(err, res, body) {
                    if (err) {
                        throw err;
                    }
                    expect(gu.isHTTPSuccess(res.statusCode)).to.equal(true);
                    gu.debugLog('POST attempt status: '+ res.statusCode, VERBOSE);

                    var cnMetadata = new dhh.channelMetadata(JSON.parse(body)),
                        uri = cnMetadata.getChannelUri(),
                        md5sum = crypto.createHash('md5'),
                        s = fs.ReadStream(CAT_TOILET_PIC);
                    gu.debugLog('URI to get image: '+ uri, VERBOSE);

                    s.on('data', function(d) {
                        md5sum.update(d);
                    }).on('end', function() {
                            var expCheckSum = md5sum.digest('hex');

                            dhh.getValidationChecksum(uri, expCheckSum, done);
                        });

                })
            );

        });
    })

    // For story:  Provide the client with a creation-timestamp in the response from a data storage request.k
    // https://www.pivotaltracker.com/story/show/43221779

    describe('Creation timestamps returned:', function() {

        it('Creation timestamp returned on data storage', function(done){

            dhh.postData(channelUri, randomPayload, function(res, uri) {
                expect(gu.isHTTPSuccess(res.status)).to.equal(true);
                var timestamp = moment(res.body.timestamp);

                expect(moment(timestamp).isValid()).to.be.true;
                done();
            });
        });

        it('Multiple POSTings of data to a channel should return ever-increasing creation timestamps.', function(done) {
            // serverTimeDiff will be set to the diff between now and initial response time plus five minutes, just to ensure there
            //      aren't any egregious shenanigans afoot. In milliseconds.
            var serverTimeDiff,
                respMoment;

            async.waterfall([
                function(callback){
                    setTimeout(function(){

                        dhh.postData(channelUri, dhh.getRandomPayload(), function(res, uri) {
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

                        dhh.postData(channelUri, dhh.getRandomPayload(), function(res, uri) {
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

                        dhh.postData(channelUri, dhh.getRandomPayload(), function(res, uri) {
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
    describe('Location header in response:', function() {

        it('Acceptance - location header exists and is correct', function(done) {

            dhh.postData(channelUri, randomPayload, function(res, uri) {
                expect(gu.isHTTPSuccess(res.status)).to.equal(true);
                var myHeader = new dhh.packetPOSTHeader(res.headers),
                    location = myHeader.getLocation();
                expect(location).to.equal(uri);

                done();
            });
        });

        it('Negative - failed attempt has no location header:', function(done) {

            dhh.postData(fakeChannelUri, randomPayload, function(res, uri) {
                expect(gu.isHTTPError(res.status)).to.equal(true);
                var myHeader = new dhh.packetPOSTHeader(res.headers),
                    location = myHeader.getLocation();
                expect(location).to.be.null;

                done();
            });
        });
    });

});