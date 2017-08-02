require('./integration_config.js');

var WebSocket = require('ws');

var channelName = utils.randomChannelName();
var channelResource = channelUrl + "/" + channelName;

describe(__filename, function () {

    utils.createChannel(channelName, null, 'websocket testing');

    var startingItem;

    it('posts item to channel', function (done) {
        utils.postItemQ(channelResource)
            .then(function (result) {
                var itemURL = result.response.headers.location;
                console.log('posted:', itemURL);
                startingItem = itemURL;
                done();
            });
    });

    var wsURL;

    it('builds websocket url', function () {
        expect(startingItem).toBeDefined();
        var itemPathComponents = startingItem.split('/');
        var itemYear = itemPathComponents[5];
        var itemMonth = itemPathComponents[6];
        var itemDay = itemPathComponents[7];
        var itemHour = itemPathComponents[8];
        var hourURL = channelResource + '/' + itemYear + '/' + itemMonth + '/' + itemDay + '/' + itemHour;
        wsURL = hourURL.replace('http', 'ws') + '/ws'
    });

    var webSocket;
    var receivedMessages = [];

    it('opens websocket', function (done) {
        expect(wsURL).toBeDefined();

        webSocket = new WebSocket(wsURL);
        webSocket.onmessage = function (message) {
            console.log('received:', message.data);
            receivedMessages.push(message.data);
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
                var itemURL = result.response.headers.location;
                console.log('posted:', itemURL);
                postedItem = itemURL;
                done();
            });
    });

    it('waits for data', function (done) {
        var sentItems = [startingItem, postedItem];
        utils.waitForData(receivedMessages, sentItems, done);
    });

    it('verifies the correct data was received', function () {
        expect(receivedMessages.length).toEqual(2);
        expect(receivedMessages).toContain(startingItem);
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
