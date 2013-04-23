/**
 * Created with JetBrains WebStorm.
 * User: gnewcomb
 * Date: 3/1/13
 * Time: 8:55 AM
 * To change this template use File | Settings | File Templates.
 */

// REPL MAGIC:  var dhh = require('/users/gnewcomb/datahub/src/test/qatests/DH_test_helpers/DHtesthelpers.js');

"use strict";

var chai = require('chai');
var expect = chai.expect;
var superagent = require('superagent');
var crypto = require('crypto');
var async = require('async');
var http = require('http');
var ws = require('ws');

var testRandom = require('../randomUtils.js');
var gu = require('../genericUtils.js');

//var URL_ROOT = 'http://10.250.220.197:8080';
var URL_ROOT = 'http://datahub-01.cloud-east.dev:8080';
exports.URL_ROOT = URL_ROOT;

var DEBUG = true;
exports.DEBUG = DEBUG;

var getValidationString = function (myUri, myPayload, myDone)
{
    var myData = '';
    http.get(myUri, function(res) {
        res.on('data', function (chunk) {
            myData += chunk;
        }).on('end', function(){
                expect(myData).to.equal(myPayload);
                myDone();
            });
    }).on('error', function(e) {
            console.log("Got error: " + e.message);
            myDone();
        });
};
exports.getValidationString = getValidationString;

var getValidationChecksum = function (myUri, expChecksum, myDone)
{
    var md5sum = crypto.createHash('md5');
    var actChecksum;

    http.get(myUri, function(res) {
        res.on('data', function (chunk) {
            md5sum.update(chunk);
        }).on('end', function(){
                actChecksum = md5sum.digest('hex');
                expect(actChecksum).to.equal(expChecksum);
                myDone();
            });
    }).on('error', function(e) {
            console.log("Got error: " + e.message);
            myDone();
        });

};
exports.getValidationChecksum = getValidationChecksum;

// Given a domain (and :port) and channel name, this will instantiate a websocket on that channel
var createWebSocket = function(domain, channelName, onOpen) {
    var wsUri = 'ws://'+ domain +'/channel/'+ channelName +'/ws';
    var myWs;

    debugLog('Trying uri: '+ wsUri, DEBUG);

    myWs = new ws(wsUri);

    myWs.on('open', onOpen);
    myWs.on('error', function(e) {console.log(e); });

    return myWs;
}
exports.createWebSocket = createWebSocket;

// Wrapper for websocket to more easily support tests with multiple sockets.
//      Yes, I know it's too-tightly coupled with the test code.
//
// domain = domain:port for the datahub, e.g., datahub-01.cloud-east.dev:8080
// channel = name of channel in the datahub
// socketName = arbitrary name to help identify the socket
// onOpenCB = callback to call when the 'open' event is called on this socket
// responseQueue = where the message is stuffed when the 'message' event is called on this socket

function WSWrapper(domain, channel, socketName, onOpenCB) {
    this.name = socketName;
    this.responseQueue = [];
    this.ws = null;
    this.channel = channel;
    this.domain = domain;
    var _self = this;

    this.onMessage = function(data, flags) {
        debugLog('MESSAGE EVENT at '+ Date.now(), DEBUG);
        debugLog('Readystate is '+ _self.ws.readyState, DEBUG);
         _self.responseQueue.push(data);
    };

    this.onOpen = function() {
        debugLog('OPEN EVENT at '+ Date.now(), DEBUG);
        debugLog('readystate: '+ _self.ws.readyState, DEBUG);
        onOpenCB();
    };

    this.createSocket = function() {
        if (DEBUG) {
            console.dir(this);
        }
        this.ws = createWebSocket(this.domain, this.channel, this.onOpen);
        this.ws.on('message', this.onMessage);
    };
};
exports.WSWrapper = WSWrapper;

// returns the POST response
var makeChannel = function(myChannelName, myCallback) {
    var myPayload = '{"name":"'+ myChannelName +'"}';
    var uri = URL_ROOT +'/channel';

    debugLog('makeChannel.uri: '+ uri, DEBUG);
    debugLog('makeChannel.payload: '+ myPayload, DEBUG);

    superagent.agent().post(uri)
        .set('Content-Type', 'application/json')
        .send(myPayload)
        .end(function(err, res) {
            myCallback(res);
        }).on('error', function(e) {
            debugLog('...in makeChannel.post.error()', DEBUG);
            myCallback(e);
        });
};
exports.makeChannel = makeChannel;

var makeRandomChannelName = function() {
    return testRandom.randomString(testRandom.randomNum(31), testRandom.limitedRandomChar);
}
exports.makeRandomChannelName = makeRandomChannelName;

//     Current metadata structure for GET on a channel:
/*
 {  _links:
 {   self:
 {   href: 'http://datahub-01.cloud-east.dev:8080/channel/philcollinssucks' },
 latest:
 {   href: 'http://datahub-01.cloud-east.dev:8080/channel/philcollinssucks/latest' }
 },
 name: 'philcollinssucks',
 creationDate: '2013-02-25T23:57:49.477Z'
 }
 */
function channelMetadata(responseBody) {
    this.getChannelUri = function() {
        return responseBody._links.self.href;
    }

    this.getLatestUri = function() {
        return responseBody._links.latest.href;
    }

    this.getName = function() {
        return responseBody.name;
    }

    this.getCreationDate = function() {
        return responseBody.creationDate;
    }
}
exports.channelMetadata = channelMetadata;

// Calls back with GET response
var getChannel = function(myChannelName, myCallback) {
    superagent.agent().get(URL_ROOT +'/channel/'+ myChannelName)
        .end(function(err, res) {
            if (err) {throw err};
            myCallback(res);
        });
};
exports.getChannel = getChannel;

/* Basic health check.
    Returns the get response or throws an error.
 */
var getHealth = function(myCallback) {
    superagent.agent().get(URL_ROOT + '/health')
        .end(function(err,res) {
            if (err) {throw err};
            myCallback(res);
        });
};
exports.getHealth = getHealth;


function packetMetadata(responseBody) {

    this.getChannelUri = function() {
        return responseBody._links.channel.href;
    } ;

    this.getPacketUri = function() {
        return responseBody._links.self.href;
    };

    this.getId = function() {
        return responseBody.id;
    };

    this.getTimestamp = function() {
        return responseBody.timestamp;
    } ;
}
exports.packetMetadata = packetMetadata;

// Headers in a response to GET on a packet of data
function packetGETHeader(responseHeader){

    // returns null if no 'previous' header found
    this.getNext = function() {
        //console.log("Called getNext() with responseHeader: ");
        //console.dir(responseHeader);

        if (responseHeader.hasOwnProperty("link")) {
            var m = /<([^<]+)>;rel=\"next\"/.exec(responseHeader["link"]);
            return (null == m) ? null : m[1];
        }
        else {
            return null;
        }
    }

    // returns null if no 'previous' header found
    this.getPrevious = function() {
        //console.log("Called getPrevious() with responseHeader: ");
        //console.dir(responseHeader);

        if (responseHeader.hasOwnProperty("link")) {
            var m = /<([^<]+)>;rel=\"previous\"/.exec(responseHeader["link"]);
            return (null == m) ? null : m[1];
        }
        else {
            return null;
        }
    }
}
exports.packetGETHeader = packetGETHeader;


// Headers in response to POSTing a packet of data
function packetPOSTHeader(responseHeader){
    this.getLocation = function() {
        if (responseHeader.hasOwnProperty("location")) {
            return responseHeader["location"];
        }
        else {
            return null;
        }
    };
}
exports.packetPOSTHeader = packetPOSTHeader;

// Posts data and returns (response, URI)
var postData = function(myChannelName, myData, myCallback) {
    var uri = URL_ROOT +'/channel/'+ myChannelName;
    var dataUri;

    superagent.agent().post(uri)
        .send(myData)
        .end(function(err, res) {
            if (err) {
                throw err;
            }

            if (!gu.isHTTPSuccess(res.status)) {
                dataUri = null;
            }
            else {
                var pMetadata = new packetMetadata(res.body);
                dataUri = pMetadata.getPacketUri();
            }

            myCallback(res, dataUri);
        });
};
exports.postData = postData;

// Returns GET response in callback
var postDataAndConfirmContentType = function(myChannelName, myContentType, myCallback) {

    var payload = testRandom.randomString(testRandom.randomNum(51));
    var uri = URL_ROOT +'/channel/'+ myChannelName;
    var getAgent = superagent.agent();
    var agent = superagent.agent();

    agent.post(uri)
        .set('Content-Type', myContentType)
        .send(payload)
        .end(function(err, res) {
            if (err) throw err;
            expect(gu.isHTTPSuccess(res.status)).to.equal(true);
            uri = res.body._links.self.href;

            getAgent.get(uri)
                .end(function(err2, res2) {
                    if (err2) throw err2;
                    expect(res2.type.toLowerCase()).to.equal(myContentType.toLowerCase());
                    myCallback(res2);
                });

        });
};
exports.postDataAndConfirmContentType = postDataAndConfirmContentType;

// Calls back with data
var getLatestDataFromChannel = function(myChannelName, myCallback) {

    var myData = '';

    getLatestUriFromChannel(myChannelName, function(uri) {
        http.get(uri, function(res) {
            res.on('data', function (chunk) {
                myData += chunk;
            }).on('end', function(){
                    myCallback(myData);
                });
        }).on('error', function(e) {
                myCallback(e);
            });
    });

    // The old version...delete once tests pass.
    /*
    var getUri = URL_ROOT +'/channel/'+ myChannelName +'/latest';

    async.waterfall([
        function(callback){
            superagent.agent().get(getUri)
                .redirects(0)
                .end(function(err, res) {
                    expect(res.status).to.equal(GET_LATEST_SUCCESS_RESPONSE);
                    expect(res.headers['location']).not.to.be.null;

                    callback(null, res.headers['location']);
                });
        },
        function(newUri, callback){
            var myData = '';

            http.get(newUri, function(res) {
                res.on('data', function (chunk) {
                    myData += chunk;
                }).on('end', function(){
                        callback(null, myData);
                    });
            }).on('error', function(e) {
                    callback(e, null);
                });
        }
    ], function (err, finalData) {
        if (err) throw err;
        myCallback(finalData);
    });
    */

};
exports.getLatestDataFromChannel = getLatestDataFromChannel;

// Calls back with the Uri for the latest set of data
var getLatestUriFromChannel = function(myChannelName, myCallback) {
    var getUri = URL_ROOT +'/channel/'+ myChannelName +'/latest';
    superagent.agent().get(getUri)
        .redirects(0)
        .end(function(err, res) {
            expect(res.status).to.equal(gu.HTTPresponses.See_Other);
            expect(res.headers['location']).not.to.be.null;

            myCallback(res.headers['location']);
        });
};
exports.getLatestUriFromChannel = getLatestUriFromChannel;

// Returns the last <reqLength> URIs from a channel as an array,
//      starting with oldest (up to reqLength) and ending with latest.
// Returns a minimum of 2 (otherwise call getLatestUriFromChannel()).
var getListOfLatestUrisFromChannel = function(reqLength, myChannelName, myCallback){
    var allUris = [];

    debugLog('In getListofLatestUrisFromChannel...', DEBUG);
    debugLog('reqLength: '+ reqLength, DEBUG);
    debugLog('myChannelName: '+ myChannelName, DEBUG);

    if (reqLength < 2) {
        reqLength = 2;
    }

    getLatestUriFromChannel(myChannelName, function(latest){
        var previous = null;

        allUris.unshift(latest);
        //console.log('Added uri:'+ latest);

        async.doWhilst(
            function (callback) {
               superagent.agent().get(allUris[0])
                    .end(function(err, res) {
                        var pGetHeader = new packetGETHeader(res.headers) ;
                        previous = pGetHeader.getPrevious();

                        if (null != previous) {
                            allUris.unshift(previous);
                            //console.log('Added uri:'+ previous);
                        }
                        callback();
                    });
            },
            function() {
                return (allUris.length < reqLength) && (null != previous);
            }
            , function(err) {
                myCallback(allUris);
            }
        );
    });
};
exports.getListOfLatestUrisFromChannel = getListOfLatestUrisFromChannel;

// writes to console log if doDebug is true *or* if only one param (msg) is provided
var debugLog = function(msg, doDebug) {
    if ((arguments.length < 2) || (true === doDebug))  {
        console.log(msg);
    }
};
exports.debugLog = debugLog;
