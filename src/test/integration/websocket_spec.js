require('./integration_config.js');

var WebSocket = require('ws');

var channelName = utils.randomChannelName();
var channelResource = channelUrl + "/" + channelName;
var testName = __filename;
var request = require('request');

var MINUTE = 60 * 1000;

describe(testName, function () {
    utils.createChannel(channelName);

    var webSocketStartingAtLatest;
    var itemUrl;

    it('starts websocket at the latest item in ' + channelName, function (done) {
        var wsUrl = channelResource.replace('http', 'ws') + '/ws';
        console.log('wsUrl', wsUrl);

        webSocketStartingAtLatest = new WebSocket(wsUrl);
        webSocketStartingAtLatest.on('open', function () {
            console.log('opened wsUrl', wsUrl);
            setTimeout(function () {
                done()
            }, 2000);
        });

    }, 15000);

    utils.addItem(channelResource);

    it('waits for data ' + channelName, function (done) {
        webSocketStartingAtLatest.onmessage = function (message) {
            itemUrl = message.data;
            console.log('messagedUrl', itemUrl);
            done();
        };
    }, MINUTE + 1);

    it('closes websocket ' + channelName, function (done) {
        webSocketStartingAtLatest.onclose = function () {
            console.log('closed ws');
            done();
        };

        webSocketStartingAtLatest.close();

    }, MINUTE + 2);

    var webSocketStartingAtItem;

    it('starts websocket at the latest item in ' + channelName, function (done) {
        var wsUrl = itemUrl.replace('http', 'ws') + '/ws';
        console.log('wsUrl', wsUrl);

        webSocketStartingAtItem = new WebSocket(wsUrl);
        webSocketStartingAtItem.on('open', function () {
            console.log('opened wsUrl', wsUrl);
            setTimeout(function () {
                done()
            }, 2000);
        });

    }, 15000);

    utils.addItem(channelResource);

    it('waits for data ' + channelName, function (done) {
        webSocketStartingAtItem.onmessage = function (message) {
            itemUrl = message.data;
            console.log('messagedUrl', itemUrl);
            done();
        };
    }, MINUTE + 1);

    it('closes websocket ' + channelName, function (done) {
        webSocketStartingAtItem.onclose = function () {
            console.log('closed ws');
            done();
        };

        webSocketStartingAtItem.close();

    }, MINUTE + 2);

});
