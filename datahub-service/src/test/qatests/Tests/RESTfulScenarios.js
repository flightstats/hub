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
    numExtraPosterBots = 10,

    // if this is true, then the test will not blow up if the main bot tries to create a channel that already exists.
    allowExistingChannel = true,
    DEBUG = true;

describe('RESTful Scenarios', function() {

    var mainChannelUri,
//        mainChannelName = dhh.getRandomChannelName();
        newChannelName = 'ActiveChannel3';

    before(function(done) {

        dhh.createChannel({name: dhh.getRandomChannelName()}, function(res, uri) {
            expect(gu.isHTTPSuccess(res.status)).to.be.true;

            mainChannelUri = uri;
            gu.debugLog('(new) main channel URI: '+ mainChannelUri, DEBUG);

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
            mainTTL = 15,
            allDependentBots = [];  // contains all bots other than mainPosterBot

        before(function() {
            mainPosterBot = bot.makeMainPosterBot(mainTTL + 10);
            mainPosterBot.channelName = newChannelName;

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

            var calculatedTimeout = (mainTTL * 1000) + 30000;
            this.timeout(calculatedTimeout);
            gu.debugLog('************   Timeout will be '+ calculatedTimeout +' milliseconds   *************');

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

        it('Create channel, list channels, get channel, insert content, get latest uri, get content', function(done) {
            var VERBOSE = false;

            channelName = dhh.getRandomChannelName();

            dhh.createChannel({name: channelName, debug: VERBOSE}, function(createRes, createdUri) {
                expect(gu.isHTTPSuccess(createRes.status)).to.be.true;

                dhh.getAllChannels({debug: VERBOSE}, function(getAllRes, allChannels) {
                    var foundCn = lodash.find(allChannels, {'name': channelName}),
                        cnUri = foundCn.href;

                    dhh.getChannel({'uri': cnUri, debug: VERBOSE}, function(getRes, getBody) {
                        expect(gu.isHTTPSuccess(getRes.status)).to.be.true;

                        var cnMetadata = new dhh.channelMetadata(getBody);
                        channelUri = cnMetadata.getChannelUri();

                        var payload = ranU.randomString(100);

                        dhh.postData({channelUri: channelUri, data: payload, debug: VERBOSE}, function(postRes, dataUri) {
                            expect(gu.isHTTPSuccess(postRes.status)).to.be.true;

                            dhh.getLatestUri(channelUri, function(latestUri, latestError) {
                                expect(latestError).to.be.null;

                                dhh.getDataFromChannel({uri: latestUri, debug: VERBOSE}, function(err, res, data) {
                                    expect(err).to.be.null;
                                    expect(data).to.equal(payload);

                                    done();
                                })
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