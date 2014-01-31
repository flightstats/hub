/**
 * Created with IntelliJ IDEA.
 * User: gnewcomb
 * Date: 9/5/13
 * Time: 4:41 PM
 * To change this template use File | Settings | File Templates.
 */

"use strict";

var superagent = require('superagent'),
    http = require('http'),
    request = require('request'),
    url = require('url'),
    lodash = require('lodash'),
    moment = require('moment'),
    fs = require('fs');

var debugLog = function(msg, doDebug) {
    if ((arguments.length < 2) || (true === doDebug))  {
        console.log(msg);
    }
};

var isHTTPSuccess = function(code) {
    return ((code >= 200) && (code < 300));
};

/**
 * Inserts data into channel.
 *
 * @param params: .channelUri, .file, .contentType=application/x-www-form-urlencoded
 * @param myCallback: response, uri to data
 */
var postFileToChannel = function(params, myCallback) {
    var dataUri = null,
        channelUri = params.channelUri,
        fileLocation = params.file,
        contentType = params.contentType || 'application/x-www-form-urlencoded',
//        payload = fs.readFileSync(fileLocation, "utf8"),
        VERBOSE = (undefined !== params.debug) ? params.debug : false;


    debugLog('Channel Uri: '+ channelUri, VERBOSE);
    debugLog('File at: '+ fileLocation, VERBOSE);
    var start = moment();

    fs.createReadStream(fileLocation).pipe(request({
        method: 'POST',
        uri: channelUri,
        headers: {
            'content-type': contentType
        }
        },
        function(err, res, body) {
            debugLog('err: '+ err);
            debugLog('status: '+ res.statusCode);
            debugLog('body: ');
            console.dir(body);

            if (body.hasOwnProperty('_links')) {
                dataUri = body._links.self.href;
            }

            /*
            if (!res.statusCode.toString().charAt(0) == '2') {
                debugLog('POST of data did not return success: '+ res.statusCode, VERBOSE);
            }
            else {
                debugLog('POST succeeded.');
                dataUri = body._links.self.href;
            }
            var delta = moment().diff(start);
            debugLog('POST took '+ delta +' milliseconds to complete.');
            */

            myCallback(res, dataUri);
        }
    ));

    /*
    superagent.agent().post(channelUri)
        .set('Content-Type', contentType)
        .send(payload)
        .end(function(err, res) {
            if (!res.status.toString().charAt(0) == '2') {
                debugLog('POST of data did not return success: '+ res.status, VERBOSE);
                dataUri = null;
            }
            else {
                debugLog('POST of data succeeded.', VERBOSE);
                dataUri = res.body._links.self.href;
            }
            var delta = moment().diff(start);
            debugLog('POST took '+ delta +' milliseconds to complete.');

            myCallback(res, dataUri);
        });
    */


};
exports.postFileToChannel = postFileToChannel;
