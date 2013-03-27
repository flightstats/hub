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

// replacing these with the HTTPresponses object below
/*
var GET_LATEST_SUCCESS_RESPONSE = 303;
exports.GET_LATEST_SUCCESS = GET_LATEST_SUCCESS_RESPONSE;

var CHANNEL_CREATION_SUCCESS_RESPONSE = 200;
exports.CHANNEL_CREATION_SUCCESS = CHANNEL_CREATION_SUCCESS_RESPONSE;

var DATA_POST_SUCCESS_RESPONSE = 200;
exports.DATA_POST_SUCCESS = DATA_POST_SUCCESS_RESPONSE;
*/



//var URL_ROOT = 'http://10.250.220.197:8080';
var URL_ROOT = 'http://datahub-01.cloud-east.dev:8080';
exports.URL_ROOT = URL_ROOT;

var DEBUG = true;
exports.DEBUG = DEBUG;

var HTTPresponses = {
    "Continue":100
    ,"Switching_Protocols":101
    ,"Processing":102
    ,"OK":200
    ,"Created":201
    ,"Accepted":202
    ,"Non-Authoritative_Information":203
    ,"No_Content":204
    ,"Reset_Content":205
    ,"Partial_Content":206
    ,"Multi-Status":207
    ,"Already_Reported":208
    ,"Low_on_Storage_Space":250
    ,"IM_Used":226
    ,"Multiple_Choices":300
    ,"Moved_Permanently":301
    ,"Found":302
    ,"See_Other":303
    ,"Not_Modified":304
    ,"Use_Proxy":305
    ,"Switch-Proxy":306
    ,"Temporary_Redirect":307
    ,"Permanent_Redirect":308
    ,"Bad_Request":400
    ,"Unauthorized":401
    ,"Payment_Required":402
    ,"Forbidden":403
    ,"Not_Found":404
    ,"Method_Not_Allowed":405
    ,"Not_Acceptable":406
    ,"Proxy_Authentication_Required":407
    ,"Request_Timeout":408
    ,"Conflict":409
    ,"Gone":410
    ,"Length_Required":411
    ,"Precondition_Failed":412
    ,"Request_Entity_Too_Large":413
    ,"Request-URI_Too_Long":414
    ,"Unsupported_Media_Type":415
    ,"Requested_Range_Not_Satisfiable":416
    ,"Expectation_Failed":417
    ,"I'm_a_teapot":418
    ,"Enhance_Your_Calm":420
    ,"Upgrade_Required":426
    ,"Precondition_Required":428
    ,"Too_Many_Requests":429
    ,"Request_Header_Fields_Too_Large":431
    ,"Internal_Server_Error":500
};
exports.HTTPresponses = HTTPresponses;

var isHTTPError = function(code) {
    return (code >= 400);
};
exports.isHTTPError = isHTTPError;

var isHTTPSuccess = function(code) {
    return ((code >= 200) && (code < 300));
};
exports.isHTTPSuccess = isHTTPSuccess;

var isHTTPRedirect = function(code) {
    return ((code >= 300) && (code < 400));
};
exports.isHTTPRedirect = isHTTPRedirect;

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

    console.log('Trying uri: '+ wsUri);

    myWs = new ws(wsUri);

    myWs.on('open', onOpen);
    myWs.on('error', function(e) {console.log(e); });

    return myWs;
}
exports.createWebSocket = createWebSocket;

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

            if (!isHTTPSuccess(res.status)) {
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
            expect(isHTTPSuccess(res.status)).to.equal(true);
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
            expect(res.status).to.equal(HTTPresponses.See_Other);
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

    console.log('In getListofLatestUrisFromChannel...');
    console.log('reqLength: '+ reqLength);
    console.log('myChannelName: '+ myChannelName);

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
