
/**
 * Created with JetBrains WebStorm.
 * User: gnewcomb
 * Date: 3/1/13
 * Time: 9:47 AM
 * To change this template use File | Settings | File Templates.
 */

// CREATE CHANNEL tests

var chai = require('chai');
var expect = chai.expect;
var superagent = require('superagent');
var request = require('request');
var moment = require('moment');
var async = require('async');
var fs = require('fs');

var dhh = require('.././DH_test_helpers/DHtesthelpers.js');
var testRandom = require('../randomUtils.js');
var gu = require('../genericUtils.js');

// DH Content Types
var appContentTypes = require('../contentTypes.js').applicationTypes;
var imageContentTypes = require('../contentTypes.js').imageTypes;
var messageContentTypes = require('../contentTypes.js').messageTypes;
var textContentTypes = require('../contentTypes.js').textTypes;

var URL_ROOT = dhh.URL_ROOT;

// Test variables that are regularly overwritten
var agent
    , payload
    , fileAsAStream
    , req
    , uri
    , contentType;

var channelName;



describe('GET data:', function() {

    before(function(myCallback){
        channelName = dhh.makeRandomChannelName();
        agent = superagent.agent();
        dhh.makeChannel(channelName, function(res){
            dhh.getChannel(channelName, function(res){
                expect(gu.isHTTPSuccess(res.status)).to.equal(true);

                console.log('Main test channel:'+ channelName);
                myCallback();
            });
        });
    });

    beforeEach(function(){
        agent = superagent.agent();
        payload = uri = req = contentType = '';
    })

    describe('returns Creation time:', function() {

        it('(Acceptance) Creation time returned in header', function(done) {
            payload = testRandom.randomString(Math.round(Math.random() * 50));

            dhh.postData(channelName, payload, function(res, packetUri) {
                expect(gu.isHTTPSuccess(res.status)).to.equal(true);

                var pMetadata = new dhh.packetMetadata(res.body);
                var timestamp = moment(pMetadata.getTimestamp());

                //console.log('packetUri: '+ packetUri);
                agent.get(packetUri)
                    .end(function(err, res){
                        expect(gu.isHTTPSuccess(res.status)).to.equal(true);
                        expect(res.header['creation-date']).to.not.be.null;
                        var returnedTimestamp = moment(res.header['creation-date']);
                        expect(returnedTimestamp.isSame(timestamp)).to.be.true;

                        done();
                    });
            });
        });

        it('Save two sets of data to one channel, and ensure correct creation timestamps on GETs', function(done) {
            var pMetadata, timestamp;

            async.series([
                function(callback){
                    dhh.postData(channelName, testRandom.randomString(testRandom.randomNum(51)), function(res, packetUri) {
                        pMetadata = new dhh.packetMetadata(res.body);
                        timestamp = moment(pMetadata.getTimestamp());

                        callback(null, {"uri":packetUri, "timestamp": timestamp});
                    });
                },
                function(callback){
                    dhh.postData(channelName, testRandom.randomString(testRandom.randomNum(51)), function(res, packetUri) {
                        pMetadata = new dhh.packetMetadata(res.body);
                        timestamp = moment(pMetadata.getTimestamp());

                        callback(null, {"uri":packetUri, "timestamp": timestamp});
                    });
                }
            ],
                function(err, rArray){
                    agent.get(rArray[0].uri)
                        .end(function(err1, res1){
                            timestamp = rArray[0].timestamp;
                            expect(gu.isHTTPSuccess(res1.status)).to.equal(true);
                            expect(res1.header['creation-date']).to.not.be.null;
                            var returnedTimestamp = moment(res1.header['creation-date']);
                            expect(returnedTimestamp.isSame(timestamp)).to.be.true;

                            //console.log(returnedTimestamp);

                            superagent.agent().get(rArray[1].uri)
                                .end(function(err2, res2) {
                                    timestamp = rArray[1].timestamp;
                                    expect(gu.isHTTPSuccess(res2.status)).to.equal(true);
                                    expect(res2.header['creation-date']).to.not.be.null;
                                    returnedTimestamp = moment(res2.header['creation-date']);
                                    expect(returnedTimestamp.isSame(timestamp)).to.be.true;

                                    //console.log(returnedTimestamp);

                                    done();
                                });
                        });
                });

        });

        it('Save data to two different channels, and ensure correct creation timestamps on GETs', function(done) {
            var pMetadata, timestamp;
            var channelA = {"name":null, "dataUri":null, "dataTimestamp":null};
            var channelB = {"name":null, "dataUri":null, "dataTimestamp":null};

            channelA.name = dhh.makeRandomChannelName();
            channelB.name = dhh.makeRandomChannelName();

            async.series([
                function(callback){
                    dhh.makeChannel(channelA.name, function(res) {
                        expect(gu.isHTTPSuccess(res.status)).to.equal(true);

                        dhh.makeChannel(channelB.name, function(res2) {
                            expect(gu.isHTTPSuccess(res2.status)).to.equal(true);
                            callback(null, null);
                        });
                    });
                },
                function(callback){
                    dhh.postData(channelA.name, testRandom.randomString(testRandom.randomNum(51)), function(res, packetUri) {
                        pMetadata = new dhh.packetMetadata(res.body);
                        timestamp = moment(pMetadata.getTimestamp());
                        channelA.dataUri = packetUri;
                        channelA.dataTimestamp = timestamp;

                        callback(null, null);
                    });
                },
                function(callback){
                    dhh.postData(channelName, testRandom.randomString(testRandom.randomNum(51)), function(res, packetUri) {
                        pMetadata = new dhh.packetMetadata(res.body);
                        timestamp = moment(pMetadata.getTimestamp());
                        channelB.dataUri = packetUri;
                        channelB.dataTimestamp = timestamp;

                        callback(null, null);
                    });
                }
            ],
                function(err, rArray){
                    agent.get(channelA.dataUri)
                        .end(function(err1, res1){
                            timestamp = channelA.dataTimestamp;
                            expect(gu.isHTTPSuccess(res1.status)).to.equal(true);
                            expect(res1.header['creation-date']).to.not.be.null;
                            var returnedTimestamp = moment(res1.header['creation-date']);
                            expect(returnedTimestamp.isSame(timestamp)).to.be.true;

                            //console.log(returnedTimestamp);

                            superagent.agent().get(channelB.dataUri)
                                .end(function(err2, res2) {
                                    timestamp = channelB.dataTimestamp;
                                    expect(gu.isHTTPSuccess(res2.status)).to.equal(true);
                                    expect(res2.header['creation-date']).to.not.be.null;
                                    returnedTimestamp = moment(res2.header['creation-date']);
                                    expect(returnedTimestamp.isSame(timestamp)).to.be.true;

                                    //console.log(returnedTimestamp);

                                    done();
                                });
                        });
                });

        });

        it('Get latest returns creation timestamp', function(done) {
            var thisChannel = dhh.makeRandomChannelName();

            dhh.payload = testRandom.randomString(testRandom.randomNum(51));

            async.waterfall([
                function(callback){
                    dhh.makeChannel(thisChannel, function(res) {
                        expect(gu.isHTTPSuccess(res.status)).to.equal(true);

                        callback(null);
                    });
                },
                function(callback){
                    dhh.postData(thisChannel, payload, function(myRes, myUri) {
                        expect(gu.isHTTPSuccess(myRes.status)).to.equal(true);
                        uri = URL_ROOT +'/channel/'+ thisChannel +'/latest';

                        agent.get(uri)
                            .end(function(err, res) {
                                expect(gu.isHTTPSuccess(res.status)).to.equal(true);
                                expect(res.header['creation-date']).to.not.be.null;

                                callback(null);
                            });
                    });
                }
            ], function (err) {
                done();
            });
        });
    });

    // Allow a client to access the most recently saved item in a channel.
// https://www.pivotaltracker.com/story/show/43222579
    describe('Access most recently saved item in channel:', function() {
        // Future tests
        /* (Future tests): if a data set expires, the 'get latest' call should respect that and reset to:
         the previous data set in the channel if one exists, or
         return a 404 if there were no other data sets
         */

        //    *Complex case*: this covers both retrieving the URI for latest data and ensuring that it yields the latest data.
        //    Verify at each step that the "most recent" URI returns what was most recently saved.
        //    Response to a channel creation *or* to a GET on the channel will include the URI to the latest resource in that channel.
        //    NOTE: response is 303 ("see other") – it's a redirect to the latest set of data stored in the channel.
        it('(Acceptance) Save sequence of data to channel, confirm that latest actually returns latest', function(done) {
            var thisChannel = dhh.makeRandomChannelName();
            var latestUri, myData;

            var payload1 = testRandom.randomString(testRandom.randomNum(51));
            var payload2 = testRandom.randomString(testRandom.randomNum(51));
            var payload3 = testRandom.randomString(testRandom.randomNum(51));

            //console.log('Payload1:'+ payload1);
            //console.log('Payload2:'+ payload2);
            //console.log('Payload3:'+ payload3);

            async.waterfall([
                function(callback){
                    dhh.makeChannel(thisChannel, function(res) {
                        expect(gu.isHTTPSuccess(res.status)).to.equal(true);
                        var cnMetadata = new dhh.channelMetadata(res.body);
                        latestUri = cnMetadata.getLatestUri();

                        callback(null);
                    });
                },
                function(callback){
                    dhh.postData(thisChannel, payload1, function(myRes, myUri) {
                        expect(gu.isHTTPSuccess(myRes.status)).to.equal(true);

                        dhh.getLatestDataFromChannel(thisChannel, function(myData) {
                            expect(myData).to.equal(payload1);

                            callback(null);
                        });
                    });
                },
                function(callback){
                    dhh.postData(thisChannel, payload2, function(myRes, myUri) {
                        expect(gu.isHTTPSuccess(myRes.status)).to.equal(true);

                        dhh.getLatestDataFromChannel(thisChannel, function(myData) {
                            expect(myData).to.equal(payload2);

                            callback(null);
                        });
                    });
                },
                function(callback){
                    dhh.postData(thisChannel, payload3, function(myRes, myUri) {
                        expect(gu.isHTTPSuccess(myRes.status)).to.equal(true);

                        dhh.getLatestDataFromChannel(thisChannel, function(myData) {
                            expect(myData).to.equal(payload3);

                            callback(null);
                        });
                    });
                }
            ], function (err) {
                done();
            });
        });

        it('Return 404 on Get Latest if channel has no data', function(done) {
            var thisChannel = testRandom.randomString(30, testRandom.limitedRandomChar);

            dhh.makeChannel(thisChannel, function(res) {
                expect(gu.isHTTPSuccess(res.status)).to.equal(true);

                uri = URL_ROOT +'/channel/'+ thisChannel +'/latest';

                agent.get(uri)
                    .end(function(err, res) {
                        expect(res.status).to.equal(404);
                        done();
                    });
            });
        });

        // regression for https://www.pivotaltracker.com/story/show/47150133
        it('Return 404 on Get Latest if channel does not exist', function(done) {
            var thisChannel = testRandom.randomString(30, testRandom.limitedRandomChar);

            uri = URL_ROOT +'/channel/'+ thisChannel +'/latest';

            agent.get(uri)
                .end(function(err, res) {
                    expect(res.status).to.equal(404);
                    done();
                });
        });


        it('Channel creation returns link to latest data set', function(done) {
            var thisChannel = dhh.makeRandomChannelName();

            dhh.makeChannel(thisChannel, function(res) {
                expect(gu.isHTTPSuccess(res.status)).to.equal(true);
                var cnMetadata = new dhh.channelMetadata(res.body);
                expect(cnMetadata.getChannelUri()).to.not.be.null;

                done();
            });

        });

        it('GET on Channel returns link to latest data set', function(done) {
            dhh.getChannel(channelName, function(res) {
                expect(gu.isHTTPSuccess(res.status)).to.equal(true);
                var cnMetadata = new dhh.channelMetadata(res.body);
                expect(cnMetadata.getChannelUri()).to.not.be.null;

                done();
            });
        });


        it('Get latest works when latest data set is an empty set, following a previous non-empty set', function(done) {
            var thisChannel = dhh.makeRandomChannelName();
            var latestUri, myData;

            payload = testRandom.randomString(testRandom.randomNum(51));

            async.waterfall([
                function(callback){
                    dhh.makeChannel(thisChannel, function(res) {
                        expect(gu.isHTTPSuccess(res.status)).to.equal(true);

                        var cnMetadata = new dhh.channelMetadata(res.body);
                        latestUri = cnMetadata.getLatestUri();

                        callback(null);
                    });
                },
                function(callback){
                    dhh.postData(thisChannel, payload, function(myRes, myUri) {
                        expect(gu.isHTTPSuccess(myRes.status)).to.equal(true);

                        dhh.getLatestDataFromChannel(thisChannel, function(myData) {
                            expect(myData).to.equal(payload);
                            callback(null);
                        });
                    });
                },
                function(callback){
                    dhh.postData(thisChannel, '', function(myRes, myUri) {
                        expect(gu.isHTTPSuccess(myRes.status)).to.equal(true);

                        dhh.getLatestDataFromChannel(thisChannel, function(myData) {
                            expect(myData).to.equal('');
                            callback(null);
                        });
                    });
                }
            ], function (err) {
                done();
            });
        });

        // As of 2/26, cannot be done (you cannot set the creation timestamp, and despite having the two POST calls done
        //  in parallel, the times aren't quite the same :(
        // TODO:  Save two sets of data with the same creation timestamp.
        //  Note: the client can't control which is the 'latest', but once the server has made that determination, it should stick.
        //  So repeated calls to this method will always return the same data set.
        it.skip('(*Not yet possible*) Internal sequence of data with same timestamp is preserved', function(done) {
            var thisChannel = dhh.makeRandomChannelName();
            var payload1 = testRandom.randomString(testRandom.randomNum(51));
            var payload2 = testRandom.randomString(testRandom.randomNum(51));
            var timestamp1, timestamp2;

            dhh.makeChannel(thisChannel, function(res) {
                expect(res.status).to.equal(dhh.CHANNEL_CREATION_SUCCESS);


                async.parallel([
                    function(callback){
                        dhh.postData(thisChannel, payload1, function(myRes, uri) {
                            expect(gu.isHTTPSuccess(myRes.status)).to.equal(true);
                            timestamp1 = moment(myRes.body.timestamp);
                            expect(moment(timestamp1).isValid()).to.be.true;
                            //console.log('time1 '+ timestamp1.valueOf());
                            console.log(myRes.body.timestamp);

                            callback(null);
                        });
                    },
                    function(callback){
                        dhh.postData(thisChannel, payload2, function(myRes, uri) {
                            expect(gu.isHTTPSuccess(myRes.status)).to.equal(true);
                            timestamp2 = moment(myRes.body.timestamp);
                            expect(moment(timestamp2).isValid()).to.be.true;
                            //console.log('time2 '+ timestamp2.valueOf());
                            console.log(myRes.body.timestamp);

                            callback(null);
                        });
                    }
                ],
                    function(err, results){
                        done();
                    }
                );
            });
        });
    });


    // Provide a client with the content type when retrieving a value. https://www.pivotaltracker.com/story/show/43221431
    describe('Content type is returned in response:', function() {
        // (acceptance)  Submit a request to save some data with a specified content type (image/jpeg, for example).
        //          Verify that the same content type is returned when retrieving the data.
        // Test where specified content type doesn't match actual content type (shouldn't matter, the DH should return specified content type).
        // Test with a range of content types.

        it('Acceptance - Content Type that was specified when POSTing data is returned on GET', function(done){

            dhh.postDataAndConfirmContentType(channelName, 'text/plain', function(res) {
                done();
            });

        });

        // application Content-Types
        it('Content-Type for application/* (19 types)', function(done){
            async.each(appContentTypes, function(ct, nullCallback) {
                //console.log('CT: '+ ct);
                dhh.postDataAndConfirmContentType(channelName, ct, function(res) {
                    nullCallback();
                });
            }, function(err) {
                if (err) {
                    throw err;
                };
                done();
            });
        });

        // image Content-Types
        it('Content-Type for image/* (7 types)', function(done){
            async.each(imageContentTypes, function(ct, nullCallback) {
                //console.log('CT: '+ ct);
                dhh.postDataAndConfirmContentType(channelName, ct, function(res) {
                    nullCallback();
                });
            }, function(err) {
                if (err) {
                    throw err;
                };
                done();
            });
        });

        // message Content-Types
        it('Content-Type for message/* (4 types)', function(done){
            async.each(messageContentTypes, function(ct, nullCallback) {
                //console.log('CT: '+ ct);
                dhh.postDataAndConfirmContentType(channelName, ct, function(res) {
                    nullCallback();
                });
            }, function(err) {
                if (err) {
                    throw err;
                };
                done();
            });
        });

        // text Content-Types
        it('Content-Type for textContentTypes/* (8 types)', function(done){
            async.each(textContentTypes, function(ct, nullCallback) {
                //console.log('CT: '+ ct);
                dhh.postDataAndConfirmContentType(channelName, ct, function(res) {
                    nullCallback();
                });
            }, function(err) {
                if (err) {
                    throw err;
                };
                done();
            });
        });


        it('Made-up legal Content-Type should be accepted and returned', function(done) {
            // Note that the DH accepts illegal Content-Types, but does require a slash between two strings, so that's
            //  the standard I'm going with.
            var myContentType = testRandom.randomString(testRandom.randomNum(10), testRandom.limitedRandomChar);
            myContentType += '/'+ testRandom.randomString(testRandom.randomNum(10), testRandom.limitedRandomChar);

            var getAgent = superagent.agent();

            payload = testRandom.randomString(Math.round(Math.random() * 50));
            uri = URL_ROOT +'/channel/'+ channelName;

            agent.post(uri)
                .set('Content-Type', myContentType)
                .send(payload)
                .end(function(err, res) {
                    if (err) throw err;
                    expect(gu.isHTTPSuccess(res.status)).to.equal(true);
                    var cnMetadata = new dhh.channelMetadata(res.body);
                    uri = cnMetadata.getChannelUri();

                    getAgent.get(uri)
                        .end(function(err2, res2) {
                            if (err2) throw err2;
                            expect(gu.isHTTPSuccess(res.status)).to.equal(true);
                            expect(res2.type.toLowerCase()).to.equal(myContentType.toLowerCase());
                            done();
                        });

                });

        });

        // TODO ? multi-part type testing?

    });

    describe('Get previous item link:', function() {

        it('(Acceptance) No Prev link with only one value set; Prev link does show on second value set.', function(done) {
            var myChannel = dhh.makeRandomChannelName();
            var firstValueUri;  // Uri for the first value set
            var pHeader;

            async.series([
                function(callback){
                    dhh.makeChannel(myChannel, function(res) {
                        expect(gu.isHTTPSuccess(res.status)).to.equal(true);

                        callback(null, null);
                    });
                },
                function(callback){
                    dhh.postData(myChannel, testRandom.randomString(testRandom.randomNum(51)), function(res, myUri) {
                        expect(gu.isHTTPSuccess(res.status)).to.equal(true);
                        firstValueUri = myUri;

                        superagent.agent().get(myUri)
                            .end(function(err, res) {
                                pHeader = new dhh.packetGETHeader(res.headers);
                                expect(pHeader.getPrevious()).to.be.null;

                                callback(null, null);
                            });
                    });
                },
                function(callback){
                    dhh.postData(myChannel, testRandom.randomString(testRandom.randomNum(51)), function(res, myUri) {
                        expect(gu.isHTTPSuccess(res.status)).to.equal(true);

                        superagent.agent().get(myUri)
                            .end(function(err, res) {
                                pHeader = new dhh.packetGETHeader(res.headers);
                                expect(pHeader.getPrevious()).to.equal(firstValueUri);

                                callback(null, null);
                            });
                    });
                }
            ],
                function(err, results){
                    done();
                });
        });

        // Create a new channel with three values in it. Starting with the latest value, confirm each prev points to the
        //  correct value and doesn't skip to the oldest.
        it('Three values in a sequence in a channel show proper Previous link behavior', function(done) {
            var myChannel = dhh.makeRandomChannelName();
            var firstValueUri, secondValueUri;
            var pHeader;

            async.series([
                function(callback){
                    dhh.makeChannel(myChannel, function(res) {
                        expect(gu.isHTTPSuccess(res.status)).to.equal(true);

                        callback(null, null);
                    });
                },
                function(callback){
                    dhh.postData(myChannel, testRandom.randomString(testRandom.randomNum(51)), function(res, myUri) {
                        expect(gu.isHTTPSuccess(res.status)).to.equal(true);
                        firstValueUri = myUri;

                        superagent.agent().get(myUri)
                            .end(function(err, res) {
                                pHeader = new dhh.packetGETHeader(res.headers);
                                expect(pHeader.getPrevious()).to.be.null;

                                callback(null, null);
                            });
                    });
                },
                function(callback){
                    dhh.postData(myChannel, testRandom.randomString(testRandom.randomNum(51)), function(res, myUri) {
                        expect(gu.isHTTPSuccess(res.status)).to.equal(true);
                        secondValueUri = myUri;

                        superagent.agent().get(myUri)
                            .end(function(err, res) {
                                pHeader = new dhh.packetGETHeader(res.headers);
                                expect(pHeader.getPrevious()).to.equal(firstValueUri);

                                callback(null, null);
                            });
                    });
                },
                function(callback){
                    dhh.postData(myChannel, testRandom.randomString(testRandom.randomNum(51)), function(res, myUri) {
                        expect(gu.isHTTPSuccess(res.status)).to.equal(true);

                        superagent.agent().get(myUri)
                            .end(function(err, res) {
                                pHeader = new dhh.packetGETHeader(res.headers);
                                expect(pHeader.getPrevious()).to.equal(secondValueUri);

                                callback(null, null);
                            });
                    });
                }
            ],
                function(err, results){
                    done();
                });
        });

        // TODO: Future: if the first value in a channel expires, the value after it in the channel should no longer show a 'prev' link.

        // TODO: Future: if the first value in a channel is deleted, the value after it in the channel should no longer show a 'prev' link.

        // TODO: Future: if a value that is not the first value in a channel expires, the value after it in the channel should
        //          accurately point its 'prev' link to the value before the just-expired value.
        // TODO: Future: if a value that is not the first value in a channel is deleted, the value after it in the channel should
        //          accurately point its 'prev' link to the value before the just-expired value.
    });

    describe('Get next item link:', function() {

        it('(Acceptance) No Next link with only one value set; Next link does show after following value set.', function(done) {
            var myChannel = dhh.makeRandomChannelName();
            var firstValueUri;
            var pHeader;

            async.series([
                function(callback){
                    dhh.makeChannel(myChannel, function(res) {
                        expect(gu.isHTTPSuccess(res.status)).to.equal(true);

                        callback(null, null);
                    });
                },
                function(callback){
                    dhh.postData(myChannel, testRandom.randomString(testRandom.randomNum(51)), function(res, myUri) {
                        expect(gu.isHTTPSuccess(res.status)).to.equal(true);
                        firstValueUri = myUri;

                        superagent.agent().get(myUri)
                            .end(function(err, res) {
                                pHeader = new dhh.packetGETHeader(res.headers);
                                expect(pHeader.getNext()).to.be.null;

                                callback(null, null);
                            });
                    });
                },
                function(callback){
                    dhh.postData(myChannel, testRandom.randomString(testRandom.randomNum(51)), function(res, myUri) {
                        expect(gu.isHTTPSuccess(res.status)).to.equal(true);

                        superagent.agent().get(firstValueUri)
                            .end(function(err, res) {
                                pHeader = new dhh.packetGETHeader(res.headers);
                                expect(pHeader.getNext()).to.equal(myUri);

                                callback(null, null);
                            });
                    });
                }
            ],
                function(err, results){
                    done();
                });
        });

        it('Check Next behavior and a value with both Next and Prev links', function(done) {
            var myChannel = dhh.makeRandomChannelName();
            var firstValueUri, secondValueUri, thirdValueUri;
            var pHeader;
            var debugThis = false;

            if (debugThis) {console.log('Channel name:'+ myChannel);}

            async.series([
                function(callback){
                    dhh.makeChannel(myChannel, function(res) {
                        expect(gu.isHTTPSuccess(res.status)).to.equal(true);

                        callback(null, null);
                    });
                },
                function(callback){
                    dhh.postData(myChannel, testRandom.randomString(testRandom.randomNum(51)), function(res, myUri) {
                        expect(gu.isHTTPSuccess(res.status)).to.equal(true);
                        firstValueUri = myUri;
                        if (debugThis) {console.log('First value at: '+ firstValueUri);}
                        callback(null,null);
                    });
                },
                function(callback){
                    dhh.postData(myChannel, testRandom.randomString(testRandom.randomNum(51)), function(res, myUri) {
                        expect(gu.isHTTPSuccess(res.status)).to.equal(true);
                        secondValueUri = myUri;
                        if (debugThis) {console.log('Second value at: '+ secondValueUri);}
                        callback(null,null);
                    });
                },
                function(callback){
                    dhh.postData(myChannel, testRandom.randomString(testRandom.randomNum(51)), function(res, myUri) {
                        expect(gu.isHTTPSuccess(res.status)).to.equal(true);
                        thirdValueUri = myUri;
                        if (debugThis) {console.log('Third value at: '+ thirdValueUri);}

                        superagent.agent().get(myUri)
                            .end(function(err, res) {
                                if (debugThis) {console.log('Getting third value.'+ myUri);}
                                pHeader = new dhh.packetGETHeader(res.headers);
                                expect(pHeader.getPrevious()).to.equal(secondValueUri);
                                expect(pHeader.getNext()).to.be.null;

                                callback(null,null);
                            });
                    });
                },
                function(callback){
                    superagent.agent().get(secondValueUri)
                        .end(function(err, res) {
                            if (debugThis) {console.log('Getting second value.'+ secondValueUri);}
                            pHeader = new dhh.packetGETHeader(res.headers);
                            expect(pHeader.getPrevious()).to.equal(firstValueUri);
                            expect(pHeader.getNext()).to.equal(thirdValueUri);

                            callback(null,null);
                        });
                },
                function(callback){
                    superagent.agent().get(firstValueUri)
                        .end(function(err, res) {
                            if (debugThis) {console.log('Getting first value.'+ firstValueUri);}
                            pHeader = new dhh.packetGETHeader(res.headers);
                            expect(pHeader.getPrevious()).to.be.null;
                            expect(pHeader.getNext()).to.equal(secondValueUri);

                            callback(null,null);
                        });
                }
            ],
                function(err, results){
                    done();
                });
        });

        // TODO: Future: if the last value in a channel expires, the value before it in the channel should no longer show a 'next' link.

        // TODO: Future: if the last value in a channel is deleted, the value before it in the channel should no longer show a 'next' link.

        // TODO: Future: if a value that is not the last value in a channel expires, the value before it in the channel should
        //          accurately point its 'next' link to the value after the just-expired value.
        // TODO: Future: if a value that is not the last value in a channel is deleted, the value before it in the channel should
        //          accurately point its 'next' link to the value after the just-deleted value.
    });
});
