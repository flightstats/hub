/**
 * Created with IntelliJ IDEA.
 * User: gnewcomb
 * Date: 5/14/13
 * Time: 8:00 AM
 * To change this template use File | Settings | File Templates.
 */

"use strict";

var chai = require('chai'),
    expect = chai.expect,
    superagent = require('superagent'),
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

var DEBUG = true;


// optional params (default value): numChannels (1), timeToLive (30 sec), name (random string),
//      logger (tbd), initialAction (randomized from behaviors), debug (false), lastAction (null)
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

    this.numChannels = (params.hasOwnProperty('numChannels')) ? params.numChannels : 1;
    this.TTL = (params.hasOwnProperty('timeToLive')) ? params.timeToLive : 30;
    this.name = (params.hasOwnProperty('name')) ? params.name : ranU.randomString(20, ranU.limitedRandomChar);
    this.description = null;
    this.behaviors = [];   // array of objects: .weight (integer) and .action (function pointer).
    this.logger = (params.hasOwnProperty('logger')) ? params.logger : this.report;
    this.dataGenerator = (params.hasOwnProperty('dataGenerator')) ? params.dataGenerator : dataGenerator100RandomChars;
    this.endTime = null;
    this.channelName = (params.hasOwnProperty('channelName')) ? params.channelName : null;
    this.socket = null;         // dhh.WSWrapper. Instantiated within the botSubscribe() method
    this.broadcastBot = null;   // handle to a different Bot that this Bot will subscribe to
    this.eventEmitter = new events.EventEmitter();

    // These are all used to enable sequential REST operations
    this.dataUri = null;        // set to the data location
    this.wsUri = null;
    this.channelUri = null
    this.latestUri = null;
    this.lastDataFetched = null;
    this.lastDataPosted = null;

    var _self = this,
        requiredParams = ['description', 'behaviors'],
        actionQueue = [],// queue of two-property objects: .wait in ms to wait before executing function, and .action, the function to run
        lastAction = (params.hasOwnProperty('lastAction')) ? params.lastAction : null,
        initialAction = (params.hasOwnProperty('initialAction')) ? params.initialAction : null,
        VERBOSE = (params.hasOwnProperty('debug')) ? params.debug : false;

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

    var describeSelf = function() {
        gu.debugLog('Bot Dump: ');
        console.dir(_self);
    }

    // Call this to add new actionObject (.wait, .action) to queue and emit the event
    var addAction = function(actionObj) {
        _self.report('...entered actionAdded()', VERBOSE);
        actionQueue.push(actionObj);
        _self.eventEmitter.emit('actionAdded');
    }

    // Called when a new action has been added
    var onNewAction = function() {
        _self.report('...entered onNewAction()', VERBOSE);

        if (moment().isAfter(_self.endTime)) {
            lastAction();
        }
        else if ((actionQueue.length > 0 ) || (_self.behaviors.length > 0)) {

            // Typically, if onNewAction() was called, there *should* be something in the actionQueue, but in the off chance
            //  that there isn't, get next action.
            var nextActionObj = (actionQueue.length > 0) ? actionQueue.shift() : getNextAction(_sel.behaviors),
                nextWait = (nextActionObj.hasOwnProperty('wait')) ? nextActionObj.wait : 0,
                nextAction = nextActionObj.action;
            nextWait = (nextWait < 1) ? 1 : nextWait;

            // First, wait at least 'nextWait' time (milliseconds)
            _self.report('...going to wait before calling next action for '+ nextWait +' milliseconds.', VERBOSE);
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
        _self.report('Socket Open!');
    }

    this.wakeUp = function() {
        _self.report('Waking up!');

        // If .channelUri isn't already set, set it to that of the .broadcastBot
        if ((null == _self.channelUri) && (null != _self.broadcastBot)) {
            _self.channelUri = _self.broadcastBot.channelUri;
        }

        if (VERBOSE) {
            describeSelf();
        }

        var nextActionObj;

        if (null != initialAction) {
            var wait = (initialAction.hasOwnProperty('minimumWait')) ? initialAction.minimumWait : 0;
            wait += (initialAction.hasOwnProperty('randomWait')) ? ranU.randomNum(initialAction.randomWait) + 1 : 0;
            nextActionObj = {'wait': wait, 'action': initialAction.action};
        }
        else {
            nextActionObj = getNextAction(_self.behaviors);
        }

        addAction(nextActionObj);
    }

    this.report = function(message, doReport) {
        var sayIt = (typeof doReport == 'undefined') ? true : doReport;

        gu.debugLog('\n"'+ this.name + '" ('+ this.description +') says:"'+ message +'"', sayIt);
    }

    // default function to call when bot is past its TTL
    this.terminate = function() {
        // notify controller and stop acting
        _self.report('...entered terminate()');

        if (null != _self.socket) {
            _self.report('...closing socket.');
            _self.socket.ws.close();
        }

        _self.eventEmitter.emit('terminate');
    }

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
        randomIndex;

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

// Helper method for all data post actions
var _botPost = function(theBot, data, callback) {

    superagent.agent().post(theBot.channelUri)
        .send(data)
        .end(function(err, res) {
            if (!gu.isHTTPSuccess(res.status)) {
                callback('Wrong status: '+ res.status);
            }
            else {
                theBot.lastDataPosted = data;

                var pMetadata = new dhh.packetMetadata(res.body);
                theBot.dataUri = pMetadata.getPacketUri();
                theBot.report('\nNew data at: '+ theBot.dataUri);
                theBot.report('Data is: '+ data);

                callback(null);
            }
        }).on('error', function(e) {
            callback(e.message);
        });
}

// Creates channel and returns error, channelUri
var botCreateChannel = function(theBot, callback) {
    var uri = [dhh.URL_ROOT, 'channel'].join('/'),
        name = (null == theBot.channelName) ? dhh.getRandomChannelName() : theBot.channelName;

    gu.debugLog('createChannel() uri: '+ uri, DEBUG);
    gu.debugLog('createChannel() name: '+ name, DEBUG);

    superagent.agent().post(uri)
        .set('Content-Type', 'application/json')
        .send({'name': name})
        .end(function(err, res) {
            if (gu.isHTTPError(res.status)) {
                callback('Wrong status: '+ res.status);
            }
            else {
                var cnMetadata = new dhh.channelMetadata(res.body),
                    location = cnMetadata.getChannelUri();

                theBot.channelUri = location;
                theBot.eventEmitter.emit('createdChannel');

                callback(null);
            }
        }).on('error', function(e) {
            callback(e.message);
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

    gu.debugLog('Subscriber is going to use this channel URI: '+ theBot.channelUri);

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
        gu.debugLog('Picked channel at random. Using: '+ cn.href);


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
                    callback(e.message);
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
        'debug': false,
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
        'debug': false,
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
        'debug': false,
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
        'debug': false,
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
        'debug': false,
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
        'debug': false,
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

