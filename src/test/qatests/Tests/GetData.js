
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

var dhh = require('.././DH_test_helpers/DHtesthelpers.js'),
    ranU = require('../randomUtils.js'),
    gu = require('../genericUtils.js');

// DH Content Types
var appContentTypes = require('../contentTypes.js').applicationTypes;
var imageContentTypes = require('../contentTypes.js').imageTypes;
var messageContentTypes = require('../contentTypes.js').messageTypes;
var textContentTypes = require('../contentTypes.js').textTypes;

var URL_ROOT = dhh.URL_ROOT;

// Test variables that are regularly overwritten
var agent
    , payload
    , req
    , uri
    , contentType;

var channelName,
    mainChannelUri;



describe('GET data:', function() {
    var randomPayload = null;

    before(function(myCallback){
        channelName = dhh.makeRandomChannelName();
        agent = superagent.agent();
        dhh.makeChannel(channelName, function(res, cnUri){
            dhh.getChannel({'name': channelName}, function(res){
                mainChannelUri = cnUri;

                expect(gu.isHTTPSuccess(res.status)).to.equal(true);
                console.log('Main test channel:'+ channelName);
                myCallback();
            });
        });
    });

    beforeEach(function(){
        agent = superagent.agent();
        payload = uri = req = contentType = '';
        randomPayload = dhh.getRandomPayload();

    })

    describe('GET data error cases: ', function() {
        var realDataId,     // Need a valid id for some content
            fakeDataId;

        // post data and save location
        before(function(done) {
            dhh.postData(mainChannelUri, randomPayload, function(res, packetUri) {
                expect(gu.isHTTPSuccess(res.status)).to.equal(true);

                var pMetadata = new dhh.packetMetadata(res.body);
                realDataId = pMetadata.getId();
                fakeDataId = realDataId.substring(0, realDataId.length - 7) + '8675309';    // Jenny, I got your number.

                done();
            });
        })

        // https://www.pivotaltracker.com/story/show/48696133
        // https://www.pivotaltracker.com/story/show/48704741
        it('getting real location id from fake channel yields 404 response', function(done) {
            uri = [URL_ROOT, 'channel', ranU.randomString(30, ranU.limitedRandomChar), realDataId].join('/');

            agent.get(uri)
                .end(function(err, res) {
                    expect(res.status).to.equal(404);
                    done();
                });
        })

        it('getting from real channel but fake location yields 404 response', function(done) {
            uri = [URL_ROOT, 'channel', channelName, fakeDataId].join('/');

            agent.get(uri)
                .end(function(err, res) {
                    expect(res.status).to.equal(404);
                    done();
                });
        })
    })

    describe('returns Creation time:', function() {

        it('(Acceptance) Creation time returned in header', function(done) {

            dhh.postData(mainChannelUri, randomPayload, function(res, packetUri) {
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
                    dhh.postData(mainChannelUri, randomPayload, function(res, packetUri) {
                        pMetadata = new dhh.packetMetadata(res.body);
                        timestamp = moment(pMetadata.getTimestamp());

                        callback(null, {"uri":packetUri, "timestamp": timestamp});
                    });
                },
                function(callback){
                    dhh.postData(mainChannelUri, dhh.getRandomPayload(), function(res, packetUri) {
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
            var channelA = {"uri": null, "name":null, "dataUri":null, "dataTimestamp":null};
            var channelB = {"uri": null, "name":null, "dataUri":null, "dataTimestamp":null};

            channelA.name = dhh.makeRandomChannelName();
            channelB.name = dhh.makeRandomChannelName();

            async.series([
                function(callback){
                    dhh.makeChannel(channelA.name, function(res, cnAUri) {
                        channelA.uri = cnAUri;
                        expect(gu.isHTTPSuccess(res.status)).to.equal(true);

                        dhh.makeChannel(channelB.name, function(res2, cnBUri) {
                            channelB.uri = cnBUri;
                            expect(gu.isHTTPSuccess(res2.status)).to.equal(true);
                            callback(null, null);
                        });
                    });
                },
                function(callback){
                    dhh.postData(channelA.uri, dhh.getRandomPayload(), function(res, packetUri) {
                        pMetadata = new dhh.packetMetadata(res.body);
                        timestamp = moment(pMetadata.getTimestamp());
                        channelA.dataUri = packetUri;
                        channelA.dataTimestamp = timestamp;

                        callback(null, null);
                    });
                },
                function(callback){
                    dhh.postData(channelB.uri, dhh.getRandomPayload(), function(res, packetUri) {
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
            var thisChannel = dhh.makeRandomChannelName(),
                channelUri;

            async.waterfall([
                function(callback){
                    dhh.makeChannel(thisChannel, function(res, cnUri) {
                        channelUri = cnUri;
                        expect(gu.isHTTPSuccess(res.status)).to.equal(true);

                        callback(null);
                    });
                },
                function(callback){
                    dhh.postData(channelUri, dhh.getRandomPayload(), function(myRes, myUri) {
                        expect(gu.isHTTPSuccess(myRes.status)).to.equal(true);

                        dhh.getLatestUri(channelUri, function(latestUri) {

                            agent.get(latestUri)
                                .end(function(err, res) {
                                    expect(gu.isHTTPSuccess(res.status)).to.equal(true);
                                    expect(res.header['creation-date']).to.not.be.null;

                                    callback(null);
                                });
                        })
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
        var thisChannelName,
            channelUri,
            latestUri,
            cnMetadata;

        // Future tests
        /* (Future tests): if a data set expires, the 'get latest' call should respect that and reset to:
         the previous data set in the channel if one exists, or
         return a 404 if there were no other data sets
         */

        beforeEach(function(done) {
            thisChannelName = dhh.makeRandomChannelName();

            dhh.makeChannel(thisChannelName, function(res, cnUri) {
                channelUri = cnUri;
                expect(gu.isHTTPSuccess(res.status)).to.equal(true);
                cnMetadata = new dhh.channelMetadata(res.body);
                latestUri = cnMetadata.getLatestUri();

                done();
            });
        })

        //    *Complex case*: this covers both retrieving the URI for latest data and ensuring that it yields the latest data.
        //    Verify at each step that the "most recent" URI returns what was most recently saved.
        //    Response to a channel creation *or* to a GET on the channel will include the URI to the latest resource in that channel.
        //    NOTE: response is 303 ("see other") – it's a redirect to the latest set of data stored in the channel.
        it('(Acceptance) Save sequence of data to channel, confirm that latest actually returns latest', function(done) {
            var payload1 = dhh.getRandomPayload(),
                payload2 = dhh.getRandomPayload(),
                payload3 = dhh.getRandomPayload();

            //console.log('Payload1:'+ payload1);
            //console.log('Payload2:'+ payload2);
            //console.log('Payload3:'+ payload3);

            async.waterfall([
                function(callback){
                    dhh.postData(channelUri, payload1, function(myRes, myUri) {
                        expect(gu.isHTTPSuccess(myRes.status)).to.equal(true);

                        dhh.getLatestDataFromChannel(channelUri, function(myData) {
                            expect(myData).to.equal(payload1);

                            callback(null);
                        });
                    });
                },
                function(callback){
                    dhh.postData(channelUri, payload2, function(myRes, myUri) {
                        expect(gu.isHTTPSuccess(myRes.status)).to.equal(true);

                        dhh.getLatestDataFromChannel(channelUri, function(myData) {
                            expect(myData).to.equal(payload2);

                            callback(null);
                        });
                    });
                },
                function(callback){
                    dhh.postData(channelUri, payload3, function(myRes, myUri) {
                        expect(gu.isHTTPSuccess(myRes.status)).to.equal(true);

                        dhh.getLatestDataFromChannel(channelUri, function(myData) {
                            expect(myData).to.equal(payload3);

                            callback(null);
                        });
                    });
                }
            ], function (err) {
                done();
            });
        });

        it.skip('Return 404 on Get Latest if channel has no data', function(done) {
            agent.get(channelUri)
                .end(function(err, res) {
                    expect(res.status).to.equal(gu.HTTPresponses.Not_Found);

                    done();
                });
        });

        // regression for https://www.pivotaltracker.com/story/show/47150133
        it('BUG: https://www.pivotaltracker.com/story/show/47150133 - Return 404 on Get Latest if channel does not exist', function(done) {
            var thisChannel = ranU.randomString(30, ranU.limitedRandomChar);

            uri = URL_ROOT +'/channel/'+ thisChannel +'/latest';

            agent.get(uri)
                .end(function(err, res) {
                    expect(res.status).to.equal(gu.HTTPresponses.Not_Found);
                    done();
                });
        });


        it('Channel creation returns link to latest data set', function() {
            expect(latestUri).to.not.be.null;

        });

        it('GET on Channel returns link to latest data set', function(done) {
            dhh.getChannel({'name': channelName} , function(res) {
                expect(gu.isHTTPSuccess(res.status)).to.equal(true);
                var cnMetadata = new dhh.channelMetadata(res.body);
                expect(cnMetadata.getLatestUri()).to.not.be.null;

                done();
            });
        });


        it('Get latest works when latest data set is an empty set, following a previous non-empty set', function(done) {
            payload = dhh.getRandomPayload();

            async.waterfall([
                function(callback){
                    dhh.postData(mainChannelUri, payload, function(myRes, myUri) {
                        expect(gu.isHTTPSuccess(myRes.status)).to.equal(true);

                        dhh.getLatestDataFromChannel(mainChannelUri, function(myData) {
                            expect(myData).to.equal(payload);
                            callback(null);
                        });
                    });
                },
                function(callback){
                    dhh.postData(mainChannelUri, '', function(myRes, myUri) {
                        expect(gu.isHTTPSuccess(myRes.status)).to.equal(true);

                        dhh.getLatestDataFromChannel(mainChannelUri, function(myData) {
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
            var payload1 = ranU.randomString(ranU.randomNum(51)),
                payload2 = ranU.randomString(ranU.randomNum(51)),
                timestamp1,
                timestamp2;

            // not implemented
        });
    });


    // Provide a client with the content type when retrieving a value. https://www.pivotaltracker.com/story/show/43221431
    describe('Content type is returned in response:', function() {
        // (acceptance)  Submit a request to save some data with a specified content type (image/jpeg, for example).
        //          Verify that the same content type is returned when retrieving the data.
        // Test where specified content type doesn't match actual content type (shouldn't matter, the DH should return specified content type).
        // Test with a range of content types.

        it('Acceptance - Content Type that was specified when POSTing data is returned on GET', function(done){

            dhh.postDataAndConfirmContentType(mainChannelUri, 'text/plain', function(res) {
                done();
            });

        });

        // application Content-Types
        it('Content-Type for application/* (19 types)', function(done){
            async.each(appContentTypes, function(ct, nullCallback) {
                //console.log('CT: '+ ct);
                dhh.postDataAndConfirmContentType(mainChannelUri, ct, function(res) {
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
                dhh.postDataAndConfirmContentType(mainChannelUri, ct, function(res) {
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
                dhh.postDataAndConfirmContentType(mainChannelUri, ct, function(res) {
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
                dhh.postDataAndConfirmContentType(mainChannelUri, ct, function(res) {
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
            var myContentType = ranU.randomString(ranU.randomNum(10), ranU.limitedRandomChar);
            myContentType += '/'+ ranU.randomString(ranU.randomNum(10), ranU.limitedRandomChar);

            var getAgent = superagent.agent();

            payload = dhh.getRandomPayload();

            agent.post(mainChannelUri)
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
        var myChannel,
            channelUri;

        beforeEach(function(done) {
            myChannel = dhh.makeRandomChannelName();

            dhh.makeChannel(myChannel, function(res, cnUri) {
                channelUri = cnUri;
                expect(gu.isHTTPSuccess(res.status)).to.equal(true);

                done();
            });
        })

        it('(Acceptance) No Prev link with only one value set; Prev link does show on second value set.', function(done) {
            var firstValueUri,
                pHeader;

            async.series([
                function(callback){
                    dhh.postData(channelUri, dhh.getRandomPayload(), function(res, myUri) {
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
                    dhh.postData(channelUri, dhh.getRandomPayload(), function(res, myUri) {
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
            var firstValueUri,
                secondValueUri,
                pHeader;

            async.series([
                function(callback){
                    dhh.postData(channelUri, dhh.getRandomPayload(), function(res, myUri) {
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
                    dhh.postData(channelUri, dhh.getRandomPayload(), function(res, myUri) {
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
                    dhh.postData(channelUri, dhh.getRandomPayload(), function(res, myUri) {
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
        var channelName,
            channelUri;

        beforeEach(function(done) {
            channelName = dhh.makeRandomChannelName();

            dhh.makeChannel(channelName, function(res, cnUri) {
                channelUri = cnUri;
                expect(gu.isHTTPSuccess(res.status)).to.equal(true);

                done();
            });
        })

        it('(Acceptance) No Next link with only one value set; Next link does show after following value set.', function(done) {
            var firstValueUri,
                pHeader;

            async.series([
                function(callback){
                    dhh.postData(channelUri, dhh.getRandomPayload(), function(res, myUri) {
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
                    dhh.postData(channelUri, dhh.getRandomPayload(), function(res, myUri) {
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
            var firstValueUri,
                secondValueUri,
                thirdValueUri,
                pHeader,
                VERBOSE = false;

            if (VERBOSE) {gu.debugLog('Channel name:'+ channelName);}

            async.series([
                function(callback){
                    dhh.postData(channelUri, dhh.getRandomPayload(), function(res, myUri) {
                        expect(gu.isHTTPSuccess(res.status)).to.equal(true);
                        firstValueUri = myUri;
                        if (VERBOSE) {gu.debugLog('First value at: '+ firstValueUri);}
                        callback(null,null);
                    });
                },
                function(callback){
                    dhh.postData(channelUri, dhh.getRandomPayload(), function(res, myUri) {
                        expect(gu.isHTTPSuccess(res.status)).to.equal(true);
                        secondValueUri = myUri;
                        if (VERBOSE) {gu.debugLog('Second value at: '+ secondValueUri);}
                        callback(null,null);
                    });
                },
                function(callback){
                    dhh.postData(channelUri, dhh.getRandomPayload(), function(res, myUri) {
                        expect(gu.isHTTPSuccess(res.status)).to.equal(true);
                        thirdValueUri = myUri;
                        if (VERBOSE) {gu.debugLog('Third value at: '+ thirdValueUri);}

                        superagent.agent().get(myUri)
                            .end(function(err, res) {
                                if (VERBOSE) {gu.debugLog('Getting third value.'+ myUri);}
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
                            if (VERBOSE) {gu.debugLog('Getting second value.'+ secondValueUri);}
                            pHeader = new dhh.packetGETHeader(res.headers);
                            expect(pHeader.getPrevious()).to.equal(firstValueUri);
                            expect(pHeader.getNext()).to.equal(thirdValueUri);

                            callback(null,null);
                        });
                },
                function(callback){
                    superagent.agent().get(firstValueUri)
                        .end(function(err, res) {
                            if (VERBOSE) {gu.debugLog('Getting first value.'+ firstValueUri);}
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
