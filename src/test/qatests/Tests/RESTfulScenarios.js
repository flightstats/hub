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
    numExtraPosterBots = 0,
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

    describe('Let loose the bots of war', function() {
        var mainPosterBot = {},
            myIntegerPosterBot = {},
            mySubscriberBot = {},
            myLatestPollBot = {},
            myRSSFeedPosterBot = {},
            myExtraPosterBots = [],
            mainTTL = 12,
            allDependentBots = [];  // contains all bots other than mainPosterBot

        before(function() {
            mainPosterBot = bot.makeMainPosterBot(mainTTL + 10);
            mainPosterBot.channelName = 'HotAndSingleRSSFeedsAreWaitingForYou';

            mySubscriberBot = bot.makeSubscriberBot(mainTTL);
            myRSSFeedPosterBot = bot.makeRSSPosterBot(mainTTL);
            myLatestPollBot = bot.makeLatestPollingBot(mainTTL);
            myIntegerPosterBot = bot.makeIntegerPosterBot(mainTTL);

            allDependentBots = [
                myIntegerPosterBot,
                mySubscriberBot,
                myRSSFeedPosterBot,
                myLatestPollBot
            ];

            for (var i = 0; i < numExtraPosterBots; i += 1) {
                var pBot = bot.makeOtherPosterBot(mainTTL);
                myExtraPosterBots[i] = pBot;
                allDependentBots.push(pBot);
            }
        })

        it('Bot madness', function(done) {
            mainPosterBot.eventEmitter.on('terminate', function() {
                expect(true).to.be.true;
                mainPosterBot.report('My last data Uri was: '+ mainPosterBot.dataUri);

                done();
            })

            mainPosterBot.eventEmitter.on('createdChannel', function() {

                console.dir(allDependentBots);

                for (var i = 0; i < allDependentBots.length; i += 1) {
                    var depBot = allDependentBots[i];
                    depBot.broadcastBot = mainPosterBot;
                    depBot.wakeUp();
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

                    dhh.postData({channelUri: channelUri, data: payload}, function(postRes, dataUri) {
                        expect(gu.isHTTPSuccess(postRes.status)).to.be.true;

                        dhh.getLatestUri(channelUri, function(latestUri, latestError) {
                            expect(latestError).to.be.null;

                            dhh.getDataFromChannel({uri: latestUri}, function(err, res, data) {
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