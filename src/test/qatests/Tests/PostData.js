// POST DATA tests

var chai = require('chai');
var expect = chai.expect;
var superagent = require('superagent');
var request = require('request');
var moment = require('moment');
var async = require('async');
var crypto = require('crypto');
var fs = require('fs');

var dhh = require('.././DH_test_helpers/DHtesthelpers.js');
var testRandom = require('.././js_testing_utils/randomUtils.js');

var URL_ROOT = dhh.URL_ROOT;

// The following paths assume this is being run from a parent directory. The before() method will adjust this if
//  the test is being run in this file's directory
var CAT_TOILET_PIC = './artifacts/cattoilet.jpg';
var MY_2MB_FILE = './artifacts/Iam2_5Mb.txt';
var MY_2KB_FILE = './artifacts/Iam200kb.txt';


// Test variables that are regularly overwritten
var agent
    , payload
    , fileAsAStream
    , req
    , uri
    , contentType;

var channelName;

before(function(myCallback){

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
    dhh.makeChannel(channelName, function(res){
        if ((res.error) || (res.status != dhh.CHANNEL_CREATION_SUCCESS)) {
            myCallback(res.error);
        };
        console.log('Main test channel:'+ channelName);
        myCallback();
    });
});

beforeEach(function(){
    agent = superagent.agent();
    payload = uri = req = contentType = '';
})


describe('POST data to channel:', function(){


    it('Acceptance - should return a (DATA_POST_SUCCESS) for POSTing data', function(done){

        payload = testRandom.randomString(Math.round(Math.random() * 50));

        dhh.postData(channelName, payload, function(res, uri) {
            expect(res.status).to.equal(dhh.DATA_POST_SUCCESS);

            dhh.getValidationString(uri, payload, done);
        });

    });


    it('POST should return a 404 trying to save to nonexistent channel', function(done){
        var myChannel =dhh.makeRandomChannelName();
        payload = testRandom.randomString(Math.round(Math.random() * 50));

        dhh.postData(myChannel, payload, function(res, uri) {
            expect(res.status).to.equal(404);
            done();
        });

    });


    it('POST same set of data twice to channel', function(done){
        payload = testRandom.randomString(Math.round(Math.random() * 50));

        dhh.postData(channelName, payload, function(res, uri) {
            expect(res.status).to.equal(dhh.DATA_POST_SUCCESS);

            dhh.postData(channelName, payload, function(res2, uri2) {
                expect(res2.status).to.equal(dhh.DATA_POST_SUCCESS);

                dhh.getValidationString(uri, payload, done);
            });
        });
    });

    it('POST same set of data to two different channels', function(done) {
        var otherChannelName = dhh.makeRandomChannelName();
        var cnMetadata, pMetadata;

        dhh.makeChannel(otherChannelName, function(res) {
            expect(res.status).to.equal(dhh.CHANNEL_CREATION_SUCCESS);
            cnMetadata = new dhh.channelMetadata(res.body);
            expect(cnMetadata.getChannelUri()).to.equal(URL_ROOT +'/channel/'+ otherChannelName);

            payload = testRandom.randomString(Math.round(Math.random() * 50));

            dhh.postData(channelName, payload, function(res, uri) {
                expect(res.status).to.equal(dhh.DATA_POST_SUCCESS);
                pMetadata = new dhh.packetMetadata(res.body);
                var actualUri = pMetadata.getPacketUri();

                dhh.getValidationString(actualUri, payload, function() {

                    dhh.postData(otherChannelName, payload, function(res2, uri2) {
                        expect(res2.status).to.equal(dhh.DATA_POST_SUCCESS);

                        dhh.getValidationString(uri2, payload, done)
                    });

                });
            });

        });
    });


    it('POST empty data set to channel', function(done){
        payload = '';

        dhh.postData(channelName, payload, function(res, uri) {
            expect(res.status).to.equal(dhh.DATA_POST_SUCCESS);

            dhh.getValidationString(uri, payload, done);
        });

    });

    it('POST 200kb file to channel', function(done) {
        payload = fs.readFileSync(MY_2KB_FILE, "utf8");

        dhh.postData(channelName, payload, function(res, uri) {
            expect(res.status).to.equal(dhh.DATA_POST_SUCCESS);

            dhh.getValidationString(uri, payload, done);
        });
    });


    it('POST 1,000 characters to channel', function(done) {
        payload = testRandom.randomString(1000, testRandom.simulatedTextChar);

        dhh.postData(channelName, payload, function(res, uri) {
            expect(res.status).to.equal(dhh.DATA_POST_SUCCESS);

            dhh.getValidationString(uri, payload, done);
        });
    });

    // Confirms via md5 checksum
    it('POST image file to channel and recover', function(done) {
        uri = URL_ROOT +'/channel/'+ channelName;

        fileAsAStream = fs.createReadStream(CAT_TOILET_PIC);

        fileAsAStream.pipe(request.post(uri,
            function(err, res, body) {
                if (err) {
                    throw err;
                }
                expect(res.statusCode).to.equal(dhh.DATA_POST_SUCCESS);
                var cnMetadata = new dhh.channelMetadata(JSON.parse(body));
                uri = cnMetadata.getChannelUri();

                var md5sum = crypto.createHash('md5');
                var s = fs.ReadStream(CAT_TOILET_PIC);

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

            var timestamp = '';

            payload = testRandom.randomString(Math.round(Math.random() * 50));
            uri = URL_ROOT +'/channel/'+ channelName;

            dhh.postData(channelName, payload, function(res, uri) {
                expect(res.status).to.equal(dhh.DATA_POST_SUCCESS);
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
                        payload = testRandom.randomString(testRandom.randomNum(51));
                        uri = URL_ROOT +'/channel/'+ channelName;

                        dhh.postData(channelName, payload, function(res, uri) {
                            expect(res.status).to.equal(dhh.DATA_POST_SUCCESS);
                            respMoment = moment(res.body.timestamp);
                            //console.log('Creation time was: '+ respMoment.format('X'));

                            expect(respMoment.isValid()).to.be.true;
                            serverTimeDiff = moment().diff(respMoment) + 300000;

                            callback(null, respMoment);
                        });
                    }, 1000);
                }
                ,function(lastResp, callback){
                    setTimeout(function(){
                        payload = testRandom.randomString(testRandom.randomNum(51));

                        dhh.postData(channelName, payload, function(res, uri) {
                            expect(res.status).to.equal(dhh.DATA_POST_SUCCESS);
                            respMoment = moment(res.body.timestamp);
                            //console.log('Creation time was: '+ respMoment.format('X'));

                            expect(respMoment.isValid()).to.be.true;
                            expect(respMoment.isAfter(lastResp)).to.be.true;
                            expect(moment().diff(respMoment)).to.be.at.most(serverTimeDiff);

                            callback(null, respMoment);
                        });
                    }, 1000);

                }
                ,function(lastResp, callback){
                    setTimeout(function(){

                        payload = testRandom.randomString(testRandom.randomNum(51));

                        dhh.postData(channelName, payload, function(res, uri) {
                            expect(res.status).to.equal(dhh.DATA_POST_SUCCESS);
                            respMoment = moment(res.body.timestamp);
                            //console.log('Creation time was: '+ respMoment.format('X'));

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
            payload = testRandom.randomString(Math.round(Math.random() * 50));

            dhh.postData(channelName, payload, function(res, uri) {
                expect(res.status).to.equal(dhh.DATA_POST_SUCCESS);
                var myHeader = new dhh.packetPOSTHeader(res.headers);
                var location = myHeader.getLocation();
                expect(location).to.equal(uri);

                done();
            });
        });

        it('Negative - failed attempt has no location header:', function(done) {
            var myChannel = dhh.makeRandomChannelName();
            payload = testRandom.randomString(Math.round(Math.random() * 50));

            dhh.postData(myChannel, payload, function(res, uri) {
                expect(res.status).to.equal(404);
                var myHeader = new dhh.packetPOSTHeader(res.headers);
                var location = myHeader.getLocation();
                expect(location).to.be.null;

                done();
            });
        });
    });

});