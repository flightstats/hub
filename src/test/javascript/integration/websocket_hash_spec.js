require('../integration_config');
const { fromObjectPath, getProp } = require('../lib/helpers');
var WebSocket = require('ws');

var channelName = utils.randomChannelName();
var channelResource = channelUrl + "/" + channelName;

describe(__filename, function () {

    utils.createChannel(channelName, null, 'websocket testing');

    var startingItem;

    it('posts item to channel', function (done) {
        utils.postItemQ(channelResource)
            .then(function (result) {
                const location = fromObjectPath(['response', 'headers', 'location'], result);
                console.log('posted:', location);
                startingItem = location;
                done();
            });
    });

    var wsURL;

    it('builds websocket url', function () {
        expect(startingItem).toBeDefined();
        wsURL = (startingItem || '').replace('http', 'ws') + '/ws';
    });

    var webSocket;
    var receivedMessages = [];

    it('opens websocket', function (done) {
        expect(wsURL).toBeDefined();

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

    var postedItem;

    it('posts item to channel', function (done) {
        utils.postItemQ(channelResource)
            .then(function (result) {
                const location = fromObjectPath(['response', 'headers', 'location'], result);
                console.log('posted:', location);
                postedItem = location;
                done();
            });
    });

    it('waits for data', function (done) {
        utils.waitForData(receivedMessages, [postedItem], done);
    });

    it('verifies the correct data was received', function () {
        expect(receivedMessages.length).toEqual(1);
        expect(receivedMessages).toContain(postedItem);
    });

    it('closes websocket', function (done) {
        webSocket.onclose = function () {
            console.log('closed:', wsURL);
            done();
        };

        webSocket.close();
    });

});
