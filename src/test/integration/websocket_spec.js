require('./../integration/integration_config.js');

var WebSocket = require('ws');

var channelName = utils.randomChannelName();
var testName = __filename;
var request = require('request');

utils.configureFrisby();

utils.runInTestChannel(testName, channelName, function (channelResponse) {

    var channelUrl = channelResponse['_links']['self']['href'];
    var wsUrl = channelResponse['_links']['ws']['href'];
    var messagedUrl = null;
    var connectionOpened = false;
    var connectionClosed = false;
    var firstDataReceived = false;
    var firstPostCompleted = false;

    console.log('wsUrl', wsUrl);
    var webSocket = new WebSocket(wsUrl);
    webSocket.on('open', function (message) {
        connectionOpened = true;

    });
    webSocket.onmessage = function (message) {
        messagedUrl = message.data;
        firstDataReceived = true;
    };
    webSocket.onclose = function () {
        connectionClosed = true;
    };

    waitsFor(function () {
        return connectionOpened;
    }, 5001);


    //post data
    var firstPostUrl = null;
    var error = null;
    runs(function () {
        request.post({url : channelUrl, headers : {"Content-Type" : "text/plain"}, body : "blahblahblah"}, function (err, response, body) {
            error = err;
            resultObj = JSON.parse(body);
            firstPostUrl = resultObj['_links']['self']['href'];
            console.log('firstPostUrl', firstPostUrl);
            firstPostCompleted = true;
        });
    });

    waitsFor(function () {
        return firstDataReceived && firstPostCompleted;
    }, 15002);

    runs(function () {
        expect(error).toBeNull();
        expect(messagedUrl).toEqual(firstPostUrl);

        webSocket.close();
        messagedUrl = 'nothing';
    });


    waitsFor(function () {
        return connectionClosed;
    }, 5003);

    var secondPostUrl = null;
    var secondPostCompleted = false;
    //post new data, verify not received
    runs(function () {
        request.post({url : channelUrl, headers : {"Content-Type" : "text/plain"}, body : "blahblahblah"}, function (err, response, body) {
            error = err;
            resultObj = JSON.parse(body);
            secondPostUrl = resultObj['_links']['self']['href'];
            secondPostCompleted = true;
        });
    });

    waitsFor(function () {
        return secondPostCompleted;
    }, 15004);


    runs(function () {
        expect(error).toBeNull();
        expect(messagedUrl).toEqual('nothing');
    });
});

