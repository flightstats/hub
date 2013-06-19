/**
 * Created with JetBrains WebStorm.
 * User: gnewcomb
 * Date: 3/1/13
 * Time: 8:55 AM
 * To change this template use File | Settings | File Templates.
 */

// REPL MAGIC:  var dhh = require('/users/gnewcomb/datahub/src/test/qatests/DH_test_helpers/DHtesthelpers.js');

"use strict";

var chai = require('chai'),
    expect = chai.expect,
    superagent = require('superagent'),
    crypto = require('crypto'),
    async = require('async'),
    http = require('http'),
    url = require('url'),
    ws = require('ws'),
    lodash = require('lodash');

var ranU = require('../randomUtils.js'),
    gu = require('../genericUtils.js');

var DOMAIN = 'datahub-01.cloud-east.dev:8080';
exports.DOMAIN = DOMAIN;

var URL_ROOT = 'http://'+ DOMAIN;
exports.URL_ROOT = URL_ROOT;

var DEBUG = false;


var getRandomPayload = function() {
    return ranU.randomString(10 + ranU.randomNum(40));
}
exports.getRandomPayload = getRandomPayload;

// Returns true if data found at dataUri matches expectedData, else false.
var confirmExpectedData = function (dataUri, expectedData, callback) {
    var actualData = '';

    http.get(dataUri, function(res) {
        res.on('data', function (chunk) {
            actualData += chunk;
        }).on('end', function(){
                if (actualData != expectedData) {
                    gu.debugLog('Unexpected data found: '+ actualData);
                }

                callback(true);
            });
    }).on('error', function(e) {
            gu.debugLog("Got error: " + e.message);
            callback(false);
        });
};
exports.confirmExpectedData = confirmExpectedData;

var getValidationChecksum = function (myUri, expChecksum, myDone)
{
    var md5sum = crypto.createHash('md5'),
        actChecksum;

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

// Given a websocket URI, this will instantiate a websocket on that channel
var createWebSocket = function(wsUri, onOpen) {
    var myWs;

    gu.debugLog('Trying uri: '+ wsUri, DEBUG);

    myWs = new ws(wsUri);

    myWs.on('open', onOpen);

    return myWs;
}
exports.createWebSocket = createWebSocket;


/**
 * Wrapper for websocket to support test scenarios
 *
 * @param params: .domain (domain:port), .channel (name of channel in DH), .socketName (arbitrary name for identifying
 *  the socket), .onOpenCB (callback to call at end of Open event), .responseQueue (where each message is stashed),.
 *  .onMessageCB (optional - callback to call at end of Message event), .onErrorCB (optional - callback to call at
 *  end of Error event), .doReconnect=false (optional: if true, will reconnect on close if due to timeout),
 *  .debug (optional).
 * @constructor
 */
function WSWrapper(params) {
    var requiredParams = ['uri', 'socketName', 'onOpenCB'];     // removed 'domain'

    lodash.forEach(requiredParams, function(p) {
        if (!params.hasOwnProperty(p)) {
            gu.debugLog('\nERROR in altWSWrapper(). Missing required param: '+ p);
        }
    })

    this.name = params.socketName;
    this.responseQueue = [];
    this.ws = null;
    this.uri = params.uri;
    //this.domain = params.domain;
    var _self = this,
        onOpenCB = params.onOpenCB,
        onMessageCB = (params.hasOwnProperty('onMessageCB')) ? params.onMessageCB : null,
        onErrorCB = (params.hasOwnProperty('onErrorCB')) ? params.onErrorCB : null,
        doReconnect = (params.hasOwnProperty('doReconnect')) ? params.doReconnect : false,
        VERBOSE = (params.hasOwnProperty('debug')) ? params.debug : DEBUG;

    this.onOpen = function() {
        gu.debugLog('OPEN EVENT at '+ Date.now(), VERBOSE);
        gu.debugLog('readystate: '+ _self.ws.readyState, VERBOSE);
        onOpenCB();
    };

    this.onMessage = function(data, flags) {
        gu.debugLog('MESSAGE EVENT at '+ Date.now(), VERBOSE);
        gu.debugLog('Readystate is '+ _self.ws.readyState, VERBOSE);
        _self.responseQueue.push(data);

        if (null != onMessageCB) {
            onMessageCB();
        }
    };

    this.onError = function(msg) {
        gu.debugLog('ERROR event at '+ Date.now(), VERBOSE);
        gu.debugLog('Error message: '+ msg);

        if (null != onErrorCB) {
            onErrorCB(msg);
        }
    };

    this.onClose = function(code, msg) {
        gu.debugLog('CLOSE event (code: '+ code +', msg: '+ msg +')\n at '+ Date.now(), true);
        if ((doReconnect) && (msg.toLowerCase().lastIndexOf('idle') > -1)) {
            gu.debugLog('...attempting reconnect after idle timeout.');
            _self.createSocket();
        }
    }

    this.createSocket = function() {
        if (VERBOSE) {
            console.dir(this);
        }
        this.ws = createWebSocket(this.uri, this.onOpen);
        this.ws.on('message', this.onMessage);
        this.ws.on('error', this.onError);
        this.ws.on('close', this.onClose);
    };
}
exports.WSWrapper = WSWrapper;

/**
 * Create a channel.
 *
 * @param params: .name, .ttl=null, .debug (optional)
 * @param myCallback: response || error, channelUri || null (if error)
 */
var createChannel = function(params, myCallback) {
    var cnName = params.name,
        ttl = (params.hasOwnProperty('ttl')) ? params.ttl : null,
        payload = '{"name":"'+ cnName +'"}',
        uri = [URL_ROOT, 'channel'].join('/'),
        VERBOSE = (params.hasOwnProperty('debug')) ? params.debug : false;

    if (null != ttl) {
        payload['ttl'] = ttl;
    }

    gu.debugLog('createChannel.uri: '+ uri, VERBOSE);
    gu.debugLog('createChannel.payload: '+ payload, VERBOSE);

    superagent.agent().post(uri)
        .set('Content-Type', 'application/json')
        .send(payload)
        .end(function(err, res) {
            if (gu.isHTTPError(res.status)) {
                myCallback(res, null);
            }
            else {
                var cnMetadata = new channelMetadata(res.body),
                    location = cnMetadata.getChannelUri();
                gu.debugLog('Created channel named: '+ cnName)
                myCallback(res, location);
            }
        }).on('error', function(e) {
            gu.debugLog('...in makeChannel.post.error()');
            myCallback(e, null);
        });
};
exports.createChannel = createChannel;

var getRandomChannelName = function() {
    return ranU.randomString( 5 + ranU.randomNum(26), ranU.limitedRandomChar);
}
exports.getRandomChannelName = getRandomChannelName;

//     Current metadata structure for GET on a channel:
function channelMetadata(responseBody) {
    this.getChannelUri = function() {
        return responseBody._links.self.href;
    }

    this.getLatestUri = function() {
        return responseBody._links.latest.href;
    }

    this.getWebSocketUri = function() {
        return responseBody._links.ws.href;
    }

    this.getName = function() {
        return responseBody.name;
    }

    this.getTTL = function() {
        throw {message: 'not implemented'}
    }

    this.getCreationDate = function() {
        return responseBody.creationDate;
    }

    this.getLastUpdateDate = function() {
        return responseBody.lastUpdateDate;
    }
}
exports.channelMetadata = channelMetadata;

/**
 * GET a channel
 *
 * @param params: .name || .uri
 * @param myCallback: response, body
 */
var getChannel = function(params, myCallback) {
    var uri,
        myChannelName = (params.hasOwnProperty('name')) ? params.name : null,
        channelUri = (params.hasOwnProperty('uri')) ? params.uri : null;

    if (null != myChannelName) {
        uri = [URL_ROOT, 'channel', myChannelName].join('/');
    }
    else if (null != channelUri) {
        uri = channelUri;
    }
    else {
        gu.debugLog('Missing required parameter in getChannel()');
    }

    gu.debugLog('\nURI for getChannel(): '+ uri);

    superagent.agent().get(uri)
        .end(function(err, res) {
            if (err) {
                throw err
            };
            myCallback(res, res.body);
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
        return (responseHeader.hasOwnProperty('location')) ? responseHeader.location : null;
    };
}
exports.packetPOSTHeader = packetPOSTHeader;

/**
 * Inserts data into channel.
 *
 * @param params: .channelUri, .data, .contentType=application/x-www-form-urlencoded (optional)
 * @param myCallback: response, uri to data
 */
var postData = function(params, myCallback) {
    var dataUri = null,
        channelUri = params.channelUri,
        myData = params.data,
        contentType = (params.hasOwnProperty('contentType')) ? params.contentType : 'application/x-www-form-urlencoded',
        VERBOSE = true;


    gu.debugLog('Channel Uri: '+ channelUri, VERBOSE);
    gu.debugLog('Data: '+ myData, VERBOSE);

    superagent.agent().post(channelUri)
        .set('Content-Type', contentType)
        .send(myData)
        .end(function(err, res) {
            if (!gu.isHTTPSuccess(res.status)) {
                gu.debugLog('POST of data did not return success: '+ res.status, VERBOSE);
                dataUri = null;
            }
            else {
                gu.debugLog('POST of data succeeded.', VERBOSE);
                var pMetadata = new packetMetadata(res.body);
                dataUri = pMetadata.getPacketUri();
            }

            myCallback(res, dataUri);
        });
};
exports.postData = postData;

// Calls back with data
var getLatestDataFromChannel = function(channelUri, callback) {

    gu.debugLog('Channel uri in getLatestDataFromChannel: '+ channelUri);

    getLatestUri(channelUri, function(uri) {

        getDataFromChannel({uri: uri}, function(err, res, data) {
            var result = (null != err) ? err : data;

            callback(result);
        })
    });
};
exports.getLatestDataFromChannel = getLatestDataFromChannel;

/**
 * Git yo data from the channel.
 *
 * @param params: .uri (to the data), .accepts (optional - split multiple options with semicolons), .debug (optional)
 * @param callback: error || null, response, data
 */
var getDataFromChannel = function(params, callback) {

    var myData = '',
        dataUri = params.uri,
        accepts = (params.hasOwnProperty('accepts')) ? params.accepts : null,
        VERBOSE = (params.hasOwnProperty('debug')) ? params.debug : false;

    gu.debugLog('DataUri in getDataFromChannel(): '+ dataUri);

    var options = url.parse(dataUri);

    if (null != accepts) {
        options['headers'] = {
            accept: accepts
        };
    }

    if (VERBOSE) {
        gu.debugLog('\nDump of options object for request: ');
        console.dir(options);
    }

    var req = http.request(options, function(res) {
        res.on('data', function (chunk) {
            myData += chunk;
        }).on('end', function(){
                gu.debugLog('Success. At end of data.', VERBOSE);

            callback(null, res, myData);
            });
    }).on('error', function(e) {
            gu.debugLog('Error in response');

            callback(e, null, null);
        });

    req.on('error', function(e) {
        gu.debugLog('Error in request');

        callback(e, null, null);
    });

    req.end();
}
exports.getDataFromChannel = getDataFromChannel;

/**
 * Gets list of all uris and their data for in a channel that come after the passed-in 'start' uri.
 *
 * @param params: .startUri, .debug (optional)
 * @param callback: err, array = [{uri: , data: }, {uri: , data: }, etc.]. Array is ordered starting with oldest data
 *          and ending with most recent.
 */
var getUrisAndDataSinceLocation = function(params, callback) {
    var startUri = params.startUri,
        dataList = [],
        VERBOSE = (params.hasOwnProperty('debug')) ? params.debug : true;

    // First Uri, get data, stash both
    getDataFromChannel({uri: startUri}, function(err, getRes, data) {
        if (err) {
            callback(err, null);
        }
        dataList.push({'uri': startUri, 'data': data});
        gu.debugLog('Added new entry.\n'+ startUri +'\n(data): '+ data, VERBOSE);

        var next = null;

        async.doWhilst(
            function(cb) {
                var uri = dataList[dataList.length - 1].uri;    // most recent item in dataList

                superagent.agent().get(uri)
                    .end(function(err, res) {
                        var pGetHeader = new packetGETHeader(res.headers) ;
                        next = pGetHeader.getNext();

                        if (null != next) {
                            getDataFromChannel({uri: next}, function(getErr, getRes, getData) {
                                if (getErr) {
                                    cb(getErr);
                                }

                                dataList.push({'uri': next, 'data': getData});
                                gu.debugLog('Added new entry.\n'+ next +'\n(data): '+ getData, VERBOSE);

                                cb();
                            })
                        }
                        else {
                            gu.debugLog('No more next headers!', VERBOSE);
                            cb();
                        }
                    });
            },
            function() {
                return (null != next);
            },
            function(err) {
                if (err) {
                    callback(err, null);
                }
                else {
                    callback(null, dataList);
                }
            }
        )
    })
}
exports.getUrisAndDataSinceLocation = getUrisAndDataSinceLocation;

// Returns the last <reqLength> URIs from a channel as an array,
//      starting with oldest (up to reqLength) and ending with latest.
// Returns a minimum of 2 (otherwise call getLatestUriFromChannel()).
var getListOfLatestUrisFromChannel = function(reqLength, channelUri, myCallback){
    var allUris = [];

    gu.debugLog('In getListofLatestUrisFromChannel...', DEBUG);
    gu.debugLog('reqLength: '+ reqLength, DEBUG);
    gu.debugLog('channel Uri: '+ channelUri, DEBUG);

    if (reqLength < 2) {
        reqLength = 2;
    }

    getLatestUri(channelUri, function(latest){
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

// Calls back with the URI to latest data post, optionally followed by error text (if not null, an error happened)
var getLatestUri = function(channelUri, callback) {
    getChannel({'uri': channelUri}, function(getCnRes, getCnBody) {
        expect(getCnRes.status).to.equal(gu.HTTPresponses.OK);

        var cnMetadata = new channelMetadata(getCnBody),
            latestUri = cnMetadata.getLatestUri();

        gu.debugLog('latestUri in getLatestUri(): '+ latestUri);

        superagent.agent().head(latestUri)
            .redirects(0)
            .end(function(headErr, headRes) {
                if (headErr) {
                    callback(null, headErr.message);
                }
                else {
                    expect(headRes.status).to.equal(gu.HTTPresponses.See_Other);
                    expect(headRes.headers['location']).to.not.be.null;

                    callback(headRes.headers.location, null);
                }
            })
    })

}
exports.getLatestUri = getLatestUri;

/**
 * Lists all channels.
 *
 * @param params: .debug (optional)
 * @param callback: response, array of channels
 */
var getAllChannels = function(params, callback) {
    var uri = [URL_ROOT, 'channel'].join('/'),
        VERBOSE = (params.hasOwnProperty('debug')) ? params.debug : false;

    gu.debugLog('uri for getAllChannels: '+ uri, VERBOSE);

    superagent.agent().get(uri)
        .end(function(err,res) {
            if (err) {throw err};

            callback(res, res.body._links.channels);
        });
}
exports.getAllChannels = getAllChannels;




