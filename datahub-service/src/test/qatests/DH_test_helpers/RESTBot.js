/**
 * Created with IntelliJ IDEA.
 * User: gnewcomb
 * Date: 5/14/13
 * Time: 8:00 AM
 * To change this template use File | Settings | File Templates.
 */

"use strict";

var superagent = require('superagent'),
    http = require('http'),
    async = require('async'),
    ws = require('ws'),
    lodash = require('lodash'),
    events = require('events'),
    moment = require('moment'),
    feed = require('feed-read');

var ranU = require('../randomUtils.js'),
    gu = require('../genericUtils.js'),
    dhh = require('./DHtesthelpers.js');

var REPORT_LEVEL = {
        CHATTY: 1,  // everything, used for debugging
        SOCIAL: 2,  // all actions, used for quick review of what the bots did and when
        SHY: 3      // only critical messages
    };

// optional params (default value): numChannels (1), timeToLive (30 sec), name (random string),
//      logger (tbd), initialAction (randomized from behaviors), reportLevel (see REPORT_LEVEL var above), lastAction (null)
//
// The entries in the .behaviors array (as well as .initialAction and .lastAction) have four properties (only two are required):
//      .weight: integer used for relative weight versus other actions in the possible action array,
//      .action:  a function that implements the behavior. This function should return a non-null error object
//      .minimumWait (optional) minimum number of milliseconds to wait before executing the action
//      .randomWait (optional) random number of milliseconds (1 through randomWait) to wait before executing the action
//      Note that either .minimumWait or .randomWait or both or none may be used
//      if a fatal error has occurred.
//      - Also note that the .behaviors array may be empty but only if .initialAction is specified (the bot will only run that)
var Bot = function Bot(params) {

    this.numChannels = (params.numChannels) ? params.numChannels : 1;
    this.TTL = (params.timeToLive) ? params.timeToLive : 30;
    this.name = (params.name) ? params.name : ranU.randomString(20, ranU.limitedRandomChar);
    this.description = null;
    this.behaviors = [];   // array of objects: .weight (integer) and .action (function pointer).
    this.logger = (params.logger) ? params.logger : this.report;
    this.dataGenerator = (params.dataGenerator) ? params.dataGenerator : dataGenerator100RandomChars;
    this.endTime = null;
    this.channelName = (params.channelName) ? params.channelName : null;
    this.socket = null;         // dhh.WSWrapper. Instantiated within the botSubscribe() method
    this.broadcastBot = null;   // handle to a different Bot that this Bot will subscribe to
    this.eventEmitter = new events.EventEmitter();
    this.reportLevel = (undefined != params.reportLevel) ? params.reportLevel : REPORT_LEVEL.SHY;;

    // These are all used to enable sequential REST operations.
    this.dataUri = null;        // set to the data location
    this.wsUri = null;
    this.channelUri = null
    this.latestUri = null;
    this.lastDataFetched = null;
    this.lastDataPosted = null;

    var _self = this,
        requiredParams = ['description', 'behaviors'],
        actionQueue = [],   // queue of objects with two properties: .wait in ms to wait before executing function, and .action, the function to run
        lastAction = (params.lastAction) ? params.lastAction : null,    // function pointer
        initialAction = (params.initialAction) ? params.initialAction : null,
        birthMoment = null,
        deathMoment = null,
        statistics = {};    // e.g., postCounts, postTotalTime, channelCreationCounts, etc.

    // Check parameters for any errors.
    lodash.forEach(requiredParams, function(p) {
        if (!params.hasOwnProperty(p)) {
            gu.debugLog('\nERROR in Bot(). Missing required param: '+ p);
        }
    })

    if ((null == initialAction) && (0 == params.behaviors.length)) {
        gu.debugLog('\n ERROR in Bot(). Either an initial action or at least one behavior must be given.');
    }

    this.description = params.description;
    this.behaviors = params.behaviors;

    // For debugging. Dumps this object to console.
    var describeSelf = function() {
        gu.debugLog('Bot Dump: ');
        console.dir(_self);
    }

    // For stats gathering. Action isn't an enum, just a string reported by each botAction function.
    this.setStat = function(action, didSucceed, time) {
        if (!statistics.hasOwnProperty(action)) {
            statistics[action] = {
                totalTime: 0,
                pass: 0,
                fail: 0
            }
        }
               
        if (didSucceed) {
            statistics[action]['pass'] += 1;
            statistics[action]['totalTime'] += time;
        }
        else {
            statistics[action]['fail'] += 1;
        }
    };

    // Call this to add new actionObject (.wait, .action) to queue and emit the event
    var addAction = function(actionObj) {
        _self.report('...entered actionAdded()', REPORT_LEVEL.CHATTY);
        actionQueue.push(actionObj);
        _self.eventEmitter.emit('actionAdded');
    }

    // Called when a new action has been added in addAction, which ends by emitting the 'actionAdded' event.
    // Checks if bot has expired.
    // Calculates wait time and waits.
    // Executes next action.
    // Calls out for next action and then calls addAction(), to continue the cycle...
    var onNewAction = function() {
        _self.report('...entered onNewAction()', REPORT_LEVEL.CHATTY);

        if (moment().isAfter(_self.endTime)) {
            lastAction();   //
        }
        else if ((actionQueue.length > 0 ) || (_self.behaviors.length > 0)) {

            // Typically, if onNewAction() was called, there *should* be something in the actionQueue, but in the off chance
            //  that there isn't, get next action.
            var nextActionObj = (actionQueue.length > 0) ? actionQueue.shift() : getNextAction(_sel.behaviors),
                nextWait = (nextActionObj.hasOwnProperty('wait')) ? nextActionObj.wait : 0,
                nextAction = nextActionObj.action;  // function pointer to the 'bot<action>' function to process
            nextWait = (nextWait < 1) ? 1 : nextWait;

            // First, wait at least 'nextWait' time (milliseconds)
            _self.report('...going to wait before calling next action for '+ nextWait +' milliseconds.', REPORT_LEVEL.CHATTY);
            setTimeout(function() {
                nextAction(_self, function(err) {
                    if (null != err) {
                        _self.report('*_*_*_*_*  ERROR *_*_*_*_*: '+ err);
                        lastAction();
                    }
                    else if ((0 == actionQueue.length) && (_self.behaviors.length > 0)) {
                        nextActionObj = getNextAction(_self.behaviors);
                        addAction(nextActionObj);
                    }
                });
            }, nextWait);
        }
    }

    this.onSocketMsg = function() {
        _self.report('Socket Message: '+ _self.socket.responseQueue[_self.socket.responseQueue.length - 1]);
        if (moment().isAfter(_self.endTime)) {
            lastAction();
        }
    }

    this.onSocketOpen = function() {
        _self.report('Socket Open!', REPORT_LEVEL.CHATTY);
    }

    // Initializer. Sets .channelUri and sets next action for the bot.
    this.wakeUp = function() {
        birthMoment = moment();
        _self.report('Waking up!');

        // If .channelUri isn't already set, set it to that of the .broadcastBot
        if ((!_self.channelUri) && (_self.broadcastBot)) {
            _self.channelUri = _self.broadcastBot.channelUri;
        }

        if (_self.reportLevel <= REPORT_LEVEL.CHATTY) {
            gu.debugLog('my report level: '+ _self.reportLevel);

            describeSelf();
        }

        var nextActionObj;

        if (initialAction) {
            var wait = (initialAction.minimumWait) ? initialAction.minimumWait : 0;
            wait += (initialAction.randomWait) ? ranU.randomNum(initialAction.randomWait) + 1 : 0;
            nextActionObj = {'wait': wait, 'action': initialAction.action};
        }
        else {
            nextActionObj = getNextAction(_self.behaviors);
        }

        addAction(nextActionObj);
    }

    /**
     * Wrapper for gu.debugLog() that does nothing special yet, other than calling that function. If the passed-in
     * reportLevel is greater than or equal to the bot's own reportLevel, then the message will be reported.
     *
     * @param message
     * @param reportLevel=REPORT_LEVEL.CHATTY if not given.
     */
    this.report = function(message, reportLevel) {
        var messageReportLevel = (undefined != reportLevel) ? reportLevel : REPORT_LEVEL.SHY,
            sayIt = (messageReportLevel >= _self.reportLevel);

        gu.debugLog('\n"'+ this.name + '" ('+ this.description +') says:"'+ message +'"', sayIt);
    }

    // Default function to call when bot is past its TTL.
    this.terminate = function() {
        // notify controller and stop acting
        _self.report('...entered terminate()', REPORT_LEVEL.CHATTY);

        if (null != _self.socket) {
            _self.report('...closing socket.', REPORT_LEVEL.SOCIAL);
            _self.socket.ws.close();
        }

        // Gather and report statistics
        deathMoment = moment();


        _self.eventEmitter.emit('terminate');
    }

    // Have to set this here as _self.terminate() doesn't exist when variables are being initialized
    if (null == lastAction) {
        lastAction = _self.terminate;
    }
    _self.eventEmitter.on('actionAdded', onNewAction);
    _self.endTime = moment().add('seconds', _self.TTL);

}
exports.Bot = Bot;

// Picks a behaviors.action at random based on adding up all behaviors.weight's
// Also calculates any up front wait time.
// Returns an object with {.wait: up front wait time in ms, .action: the function}
var getNextAction = function(behaviors) {
    var VERBOSE = false;

    gu.debugLog('...entering getNextAction()', VERBOSE);

    var max = 0,
        randomIndex = -1;

    for (var i = 0; i < behaviors.length; i += 1) {
        var weight = behaviors[i].weight;
            max += weight;
    }

    randomIndex = ranU.randomNum(max) + 1;
    gu.debugLog('randomIndex is '+ randomIndex, false);

    for (i = 0; i < behaviors.length; i += 1) {
        var behave = behaviors[i],
            weight = behave.weight,
            minimumWait = (behave.hasOwnProperty('minimumWait')) ? behave.minimumWait : 0,
            randomWait = (behave.hasOwnProperty('randomWait')) ? ranU.randomNum(behave.randomWait) + 1 : 0,
            totalWait = minimumWait + randomWait;

        gu.debugLog('weight for this behavior is '+ weight, false);

        if (randomIndex <= weight) {
            return {'wait': totalWait, 'action': behave.action};
        }
    }
}


// *********  botActions ********

// Helper method for all data post actions
var _botPost = function(theBot, data, callback) {
    var start = moment(),
        delta = -1,
        name = 'post';

    superagent.agent().post(theBot.channelUri)
        .send(data)
        .end(function(err, res) {
            if (!gu.isHTTPSuccess(res.status)) {
                theBot.setStat(name, false, 0);

                callback('Wrong status in _botPost(): '+ res.status);
            }
            else {
                delta = moment().diff(start);
                theBot.setStat(name, true, delta);

                theBot.lastDataPosted = data;

                var pMetadata = new dhh.packetMetadata(res.body);
                theBot.dataUri = pMetadata.getPacketUri();
                theBot.report('\nNew data at: '+ theBot.dataUri);
                theBot.report('Data is: '+ data, REPORT_LEVEL.CHATTY);

                callback(null);
            }
        }).on('error', function(e) {
            callback('in _botPost(): '+ e.message);
        });
}

// Creates channel and returns error, channelUri
var botCreateChannel = function(theBot, callback) {
    var uri = [dhh.URL_ROOT, 'channel'].join('/'),
        name = (null == theBot.channelName) ? dhh.getRandomChannelName() : theBot.channelName;

    gu.debugLog('createChannel() uri: '+ uri, REPORT_LEVEL.CHATTY);
    gu.debugLog('createChannel() name: '+ name, REPORT_LEVEL.CHATTY);

    superagent.agent().post(uri)
        .set('Content-Type', 'application/json')
        .send({'name': name})
        .end(function(err, res) {
            if (gu.isHTTPError(res.status)) {
                callback('Wrong status in botCreateChannel(): '+ res.status);
            }
            else {  // Created successfully
                var cnMetadata = new dhh.channelMetadata(res.body),
                    location = cnMetadata.getChannelUri();

                theBot.channelUri = location;
                theBot.eventEmitter.emit('createdChannel');
                theBot.report('Created channel at: '+ location);

                callback(null);
            }
        }).on('error', function(e) {
            callback('in botCreateChannel(): '+ e.message);
        });
}
exports.botCreateChannel = botCreateChannel;

// Posts data and returns error or null
var botPostData = function(theBot, callback) {
    var dataUri = null,
        data = theBot.dataGenerator(theBot).toString(),
        VERBOSE = false;

    gu.debugLog('Channel Uri: '+ theBot.channelUri, VERBOSE);
    gu.debugLog('Data: '+ data, VERBOSE);

    _botPost(theBot, data, callback);
}
exports.botPostData = botPostData;

// Gets the FlightStats airport delay RSS feed and posts each entry's content.
//
var botReportRSSFeed = function(theBot, callback) {

    feed('http://www.flightstats.com/go/rss/airportdelays.do;jsessionid=7A8CE765F8E532ADEE7D6C4505BDD9F4.web2:8009',
        function(err, articles) {
            async.each(
                articles,

                // For each article, post it. If an error is returned, bail out right away.
                function(article, cb) {

                    _botPost(theBot, article.content, cb);
                },
                function(err){
                    if ((typeof err != 'undefined') && (null != err)) {
                        callback(err);
                    }
                    else {
                        callback(null);
                    }
                }
            )
    })
}
exports.botReportRSSFeed = botReportRSSFeed;

var botSubscribe = function(theBot, callback) {

    theBot.report('Subscriber is going to use this channel URI: '+ theBot.channelUri);

    dhh.getChannel({'uri': theBot.channelUri}, function(getRes, getBody) {
        var cnMetadata = new dhh.channelMetadata(getBody),
            wsUri = cnMetadata.getWebSocketUri();

        theBot.wsUri = wsUri;
        theBot.socket = new dhh.WSWrapper({
                'uri': theBot.wsUri,
                'socketName': 'ws_'+ theBot.name,
                'onOpenCB': theBot.onSocketOpen,
                'onMessageCB': theBot.onSocketMsg
                });
        theBot.socket.createSocket();

        callback(null);
    })
}
exports.botSubscribe = botSubscribe;

/**
 * Uses getAllChannels to get list of all channels, then picks one at random, setting self.channelUri to its location.
 *
 * @param theBot
 * @param callback
 */
var botSelectRandomChannel = function(theBot, callback) {

    dhh.getAllChannels({}, function(res, channels) {
        if (!gu.isHTTPSuccess(res.status)) {
            callback('Error getting all channels. HTTP response: '+ res.status);
        }

        var cn = ranU.getRandomArrayElement(channels);
        theBot.channelUri = cn.href;
        theBot.report('Picked channel at random. Using: '+ cn.href);

        callback(null);
    })
}
exports.botSelectRandomChannel = botSelectRandomChannel;

var botGetLatestValue = function(theBot, callback) {

    dhh.getLatestUri(theBot.channelUri, function(uri, getErr) {
        var myData = '';

        if (null != getErr) {
            callback('Error getting latest URI: '+ getErr);
        }
        else {
            theBot.latestUri = uri;
            theBot.report('My latest URI is: '+ theBot.latestUri);

            http.get(uri, function(res) {
                res.on('data', function (chunk) {
                    myData += chunk;
                }).on('end', function(){
                        theBot.lastData = myData;

                        callback(null);
                    });
            }).on('error', function(e) {
                    callback('in botGetLatestValue(): '+ e.message);
                });
        }
    });
}
exports.botGetLatestValue = botGetLatestValue;

// bot will start with .latestUri and then fetch all since then by following
//  'Next' headers, reporting on each uri and data found
var botGetAllValuesSinceDataUri = function(theBot, callback) {

}



var makeMainPosterBot = function(TTL) {
    var params = {
        'name': 'Marvin',
        'initialAction': {'action': botCreateChannel},
        'reportLevel': REPORT_LEVEL.SHY,
        'timeToLive': TTL,
        'description': 'I am a main Poster Bot',
        'behaviors': [
            {'weight': 1, 'action': botPostData, 'minimumWait': 500, 'randomWait': 1000}
        ]
    };

    return new Bot(params);
}
exports.makeMainPosterBot = makeMainPosterBot;

var makeOtherPosterBot = function(TTL) {
    var params = {
        'name': ranU.randomString(10, ranU.limitedRandomChar),
        'reportLevel': REPORT_LEVEL.SHY,
        'timeToLive': TTL,
        'description': 'I am another Poster Bot',
        'behaviors': [
            {'weight': 1, 'action': botPostData, 'minimumWait': 50, 'randomWait': 50}
        ]
    };

    return new Bot(params);
}
exports.makeOtherPosterBot = makeOtherPosterBot;

var makeIntegerPosterBot = function(TTL) {
    var params = {
        'name': ranU.randomString(10, ranU.limitedRandomChar),
        'reportLevel': REPORT_LEVEL.SHY,
        'timeToLive': TTL,
        'description': 'I am an Integer Poster Bot',
        'dataGenerator': dataGeneratorNextInteger,
        'behaviors': [
            {'weight': 1, 'action': botPostData, 'minimumWait': 50, 'randomWait': 50}
        ]
    };

    return new Bot(params);
}
exports.makeIntegerPosterBot = makeIntegerPosterBot;

var makeRSSPosterBot = function(TTL) {

    var params = {
        'name': ranU.randomString(10, ranU.limitedRandomChar),
        'reportLevel': REPORT_LEVEL.SHY,
        'timeToLive': TTL,
        'description': 'I am an RSS Feed poster',
        'dataGenerator': dataGeneratorNextInteger,
        'behaviors': [
            {'weight': 1, 'action': botReportRSSFeed, 'minimumWait': 5000, 'randomWait': 3000}
        ]
    };

    return new Bot(params);
}
exports.makeRSSPosterBot = makeRSSPosterBot;

var makeSubscriberBot = function(TTL) {
    var params = {
        'name': 'Rosie',
        'initialAction': {'minimumWait':1000, 'action': botSubscribe},
        'reportLevel': REPORT_LEVEL.SHY,
        'timeToLive': TTL,
        'description': 'Subscriber Bot',
        'behaviors': []
    };

    return new Bot(params);
}
exports.makeSubscriberBot = makeSubscriberBot;

var makeLatestPollingBot = function(TTL) {
    var params = {
        'name': 'Paulie',
        'reportLevel': REPORT_LEVEL.SHY,
        'timeToLive': TTL,
        'description': 'Poll-for-latest Bot',
        'behaviors': [
            {'weight': 1, 'minimumWait': 8000, 'action': botGetLatestValue}
        ]
    };

    return new Bot(params);
}
exports.makeLatestPollingBot = makeLatestPollingBot;



var dataGeneratorNextInteger = function(theBot) {

    if (null == theBot.lastDataPosted) {
        theBot.lastDataPosted = 0;
    }

    theBot.lastDataPosted = parseInt(theBot.lastDataPosted) + 1;

    return theBot.lastDataPosted;
}
exports.dataGeneratorNextInteger = dataGeneratorNextInteger;

var dataGenerator100RandomChars = function(theBot) {
    return ranU.randomString(100, ranU.simulatedTextChar);
}
exports.dataGenerator100RandomChars = dataGenerator100RandomChars;

