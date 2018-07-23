require('../integration_config');
const { fromObjectPath, getProp } = require('../lib/helpers');
var WebSocket = require('ws');

var channelName = utils.randomChannelName();
var channelResource = channelUrl + "/" + channelName;

describe(__filename, function () {

    utils.createChannel(channelName, null, 'websocket testing');

    var webSocket;
    var wsURL = channelResource.replace('http', 'ws') + '/ws';
    var receivedMessages = [];

    it('opens websocket', function (done) {
        expect(wsURL).not.toEqual('undefined');

        webSocket = new WebSocket(wsURL);
        webSocket.onmessage = function (message) {
            const data = getProp('data', message);
            console.log('received:', data);
            receivedMessages.push(data);
        };

        webSocket.on('open', function () {
            console.log('opened:', wsURL);
            setTimeout(done, 5000);
        });
    });

    var itemURLs = [];

    it('posts item to channel', function (done) {
        utils.postItemQ(channelResource)
            .then(function (result) {
                const location = fromObjectPath(['response', 'headers', 'location'], result);
                console.log('posted:', location);
                itemURLs.push(location);
                done();
            });
    });

    it('waits for data', function (done) {
        utils.waitForData(receivedMessages, itemURLs, done);
    });

    it('verifies the correct data was received', function () {
        expect(receivedMessages.length).toEqual(itemURLs.length);
        for (var i = 0; i < itemURLs.length; ++i) {
            expect(receivedMessages).toContain(itemURLs[i]);
        }
    });

    it('closes websocket', function (done) {
        webSocket.onclose = function () {
            console.log('closed:', wsURL);
            done();
        };

        webSocket.close();
    });

});
