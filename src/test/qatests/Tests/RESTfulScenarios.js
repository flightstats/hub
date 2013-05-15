// RESTful scenarios

var chai = require('chai');
var expect = chai.expect;
var superagent = require('superagent');
var async = require('async');
var lodash = require('lodash');

var dhh = require('.././DH_test_helpers/DHtesthelpers.js'),
    ranU = require('../randomUtils.js'),
    gu = require('../genericUtils.js'),
    bot = require('.././DH_test_helpers/RESTBot.js');

var channelName,
    channelUri,
    numExtraPosterBots = 3,
    DEBUG = true;

describe('IN PROGRESS - Restful Scenarios', function() {

    var preMadeChannelUri;

    before(function(done) {

        dhh.createChannel(dhh.getRandomChannelName(), function(res, uri) {
            expect(gu.isHTTPSuccess(res.status)).to.be.true;

            preMadeChannelUri = uri;
            gu.debugLog('pre-made channel URI: '+ preMadeChannelUri, DEBUG);

            done();
        })
    })

    describe.skip('Let loose the bots of war', function() {
        var mainPosterBot,
            myExtraPosterBots = [],
            mySubscriberBot,
            myLatestPollBot,
            mainTTL = 12;



        before(function() {
            mainPosterBot = bot.makeMainPosterBot(mainTTL + 10);
            mySubscriberBot = bot.makeSubscriberBot(mainTTL);
            myLatestPollBot = bot.makeLatestPollingBot(mainTTL);

            for (var i = 0; i < numExtraPosterBots; i += 1) {
                var pBot = bot.makeOtherPosterBot(mainTTL);
                pBot.broadcastBot = mainPosterBot;
                myExtraPosterBots[i] = pBot;
            }

            mySubscriberBot.broadcastBot = mainPosterBot;
            myLatestPollBot.broadcastBot = mainPosterBot;
        })

        it('Bot madness', function(done) {
            mainPosterBot.eventEmitter.on('terminate', function() {
                expect(true).to.be.true;
                mainPosterBot.report('My last data Uri was: '+ mainPosterBot.dataUri);

                done();
            })

            mainPosterBot.eventEmitter.on('createdChannel', function() {
                mySubscriberBot.wakeUp();
                myLatestPollBot.wakeUp();

                for (var i = 0; i < myExtraPosterBots.length; i += 1) {
                    var pBot = myExtraPosterBots[i];
                    pBot.wakeUp();
                }
            })

            mainPosterBot.wakeUp();

        })
    })

    describe('Standard scenarios: ', function() {

        it('Create channel, get channel, insert content, get latest uri, get content', function(done) {
            channelName = dhh.getRandomChannelName();

            dhh.createChannel(channelName, function(createRes, createdUri) {
                expect(gu.isHTTPSuccess(createRes.status)).to.be.true;

                dhh.getChannel({'uri': createdUri}, function(getRes, getBody) {
                    expect(gu.isHTTPSuccess(getRes.status)).to.be.true;

                    var cnMetadata = new dhh.channelMetadata(getBody);
                    channelUri = cnMetadata.getChannelUri();

                    var payload = ranU.randomString(100);

                    dhh.postData(channelUri, payload, function(postRes, dataUri) {
                        expect(gu.isHTTPSuccess(postRes.status)).to.be.true;

                        dhh.getLatestUri(channelUri, function(latestUri, latestError) {
                            expect(latestError).to.be.null;

                            dhh.getDataFromChannel(latestUri, function(err, data) {
                                expect(err).to.be.null;
                                expect(data).to.equal(payload);

                                done();
                            })
                        })
                    })
                })
            })
        })

        it.skip('NOT IMPLEMENTED: HEAD on data in the midst of a series of data in a channel, walk Next links to end', function(done) {

        })

        it.skip('NOT IMPLEMENTED: Get channel, subscribe', function(done) {

        })
    })

})