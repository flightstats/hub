// RESTful scenarios

var chai = require('chai');
var expect = chai.expect;
var superagent = require('superagent');
var async = require('async');
var lodash = require('lodash');
var moment = require('moment');

var dhh = require('.././DH_test_helpers/DHtesthelpers.js'),
    ranU = require('../randomUtils.js'),
    gu = require('../genericUtils.js'),
    bot = require('.././DH_test_helpers/RESTBot.js');

var channelName,
    channelUri,
    numExtraPosterBots = 10,
    mainTTLSeconds = 25,
    DEBUG = true;

describe('RESTful Scenarios', function() {

    var aggregateStats = {
            totalBots: 0,
            totalBotLifetime: 0,
            runTime: 0,
            totalActions: 0
        },    // Stats aggregated by action
        start = null,
        mainChannelName = 'ActiveChannel21';


    var reportStats = function(theStats) {
        gu.debugLog('************   Statistics - by bot  *************************************');
        lodash.forOwn(theStats, function(val, botName) {
            var lifetime = val['death'].diff(val['birth']);
            aggregateStats.totalBots += 1;
            aggregateStats.totalBotLifetime += lifetime;

            gu.debugLog('Stats for bot "'+ botName +'":');
            gu.debugLog('Lifetime (ms): '+ lifetime);

            lodash.forOwn(val['statistics'], function(val, action) {
                var pass = Number(val['pass']),
                    fail = Number(val['fail']),
                    totalTime = Number(val['totalTime']);

                gu.debugLog(action);
                gu.debugLog('Successful attempts: '+ pass);
                gu.debugLog('Failed attempts: '+ fail);
                gu.debugLog('Total time (ms) for all successful attempts: '+ totalTime);
                gu.debugLog('Avg time per successful attempt: '+ totalTime / pass +'\n');
                gu.debugLog('% lifetime spent on action: '+ (Math.round((totalTime / lifetime) * 1000))/10 +'%');

                aggregateStats.totalActions += (pass + fail);

                if (!aggregateStats.hasOwnProperty(action)) {
                    aggregateStats[action] = {
                        'totalTime': totalTime,
                        'pass': pass,
                        'fail': fail
                    }
                }
                else {
                    aggregateStats[action]['totalTime'] += totalTime;
                    aggregateStats[action]['pass'] += pass;
                    aggregateStats[action]['fail'] += fail;
                }
            })

            gu.debugLog('*******************************************************');
        })

        var testRunSeconds = Math.round(aggregateStats.runTime / 1000);

        gu.debugLog('************   Statistics - aggregated  *************************************');
        gu.debugLog('Total test run (from creating first channel to end) in seconds: '+ testRunSeconds);
        gu.debugLog('Total actions taken: '+ aggregateStats.totalActions);
        gu.debugLog('Average number of actions per second: '+ aggregateStats.totalActions / testRunSeconds);
        gu.debugLog('Total number of bots: '+ aggregateStats.totalBots);
        gu.debugLog('Average bot lifetime: '+ Math.round(aggregateStats.totalBotLifetime / aggregateStats.totalBots));

        lodash.forOwn(aggregateStats, function(val, action) {
            if (
                !lodash.contains(['totalBots', 'totalBotLifetime', 'runTime', 'totalActions'], action)
//                ('totalBots' != action) && ('totalBotLifetime' != action)
                ) {
                var pass = Number(aggregateStats[action]['pass']),
                    fail = Number(aggregateStats[action]['fail']),
                    totalTime = Number(aggregateStats[action]['totalTime']);

                gu.debugLog('\nAction: '+ action);
                gu.debugLog(pass +' successful attempts');
                gu.debugLog(fail +' failed attempts');
                gu.debugLog('total time (ms): '+ totalTime);
                gu.debugLog('average time per successful attempt (ms): '+ Math.round(totalTime / pass));
            }
        })
    }

    describe('Let loose the bots of war', function() {
        var mainPosterBot = null,
            myIntegerPosterBot = null,
            mySubscriberBot = null,
            myLatestPollBot = null,
            myRSSFeedPosterBot = null,
            myExtraPosterBots,
            allDependentBots,  // contains all bots other than mainPosterBot
            allStatistics;

        before(function() {
            mainPosterBot = bot.makeMainPosterBot(mainTTLSeconds + 15);
            mainPosterBot.channelName = mainChannelName;

            mySubscriberBot = bot.makeSubscriberBot(mainTTLSeconds);
            myRSSFeedPosterBot = bot.makeRSSPosterBot(mainTTLSeconds);
            myLatestPollBot = bot.makeLatestPollingBot(mainTTLSeconds);
            myIntegerPosterBot = bot.makeIntegerPosterBot(mainTTLSeconds);

            // Register the bots
            allDependentBots = [
                myIntegerPosterBot,
                mySubscriberBot,
                myRSSFeedPosterBot,
                myLatestPollBot
            ];
            myExtraPosterBots = [];

            for (var i = 0; i < numExtraPosterBots; i += 1) {
                var pBot = bot.makeOtherPosterBot(mainTTLSeconds);
                myExtraPosterBots[i] = pBot;
                allDependentBots.push(pBot);
            }
        })

        it('Bot madness', function(done) {

            var calculatedTimeout = (mainTTLSeconds * 1000) + 30000;
            this.timeout(calculatedTimeout);
            gu.debugLog('************   Timeout will be '+ calculatedTimeout +' milliseconds   *************');

            // Called by either the createdChannel or confirmedChannel event fired by mainPosterBot
            var mainTest = function() {
                var depBot;

                if ('undefined' == typeof allStatistics) {
                    allStatistics = {};
                }

                console.dir(allDependentBots);
                start = moment();

                for (var i = 0; i < allDependentBots.length; i += 1) {
                    depBot = allDependentBots[i];
                    depBot.broadcastBot = mainPosterBot;

                    // Have to wrap the assignment of event listener in a closure to ensure that it is made for each
                    //      bot instead of only the last one.
                    //  http://stackoverflow.com/questions/8909652/adding-click-event-listeners-in-loop
                    (function (_bot) {
                        _bot.eventEmitter.on('terminate', function() {
                            allStatistics[_bot.name] = _bot.getAllStats();
                            _bot.report('Wrote stats!');
                        })
                    })(depBot);

                    depBot.wakeUp();
                }
            }

            mainPosterBot.eventEmitter.on('terminate', function() {
                aggregateStats.runTime = moment().diff(start);
                expect(true).to.be.true;
                mainPosterBot.report('My last data Uri was: '+ mainPosterBot.dataUri);
                allStatistics[mainPosterBot.name] = mainPosterBot.getAllStats();
                reportStats(allStatistics);

                done();
            })

            mainPosterBot.eventEmitter.on('createdChannel', mainTest);
            mainPosterBot.eventEmitter.on('confirmedChannel', mainTest);

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