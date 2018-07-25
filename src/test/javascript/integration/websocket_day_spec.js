require('../integration_config');
const { createChannel, fromObjectPath, getProp } = require('../lib/helpers');
var WebSocket = require('ws');
let createdChannel = false;
var channelName = utils.randomChannelName();
var channelResource = channelUrl + "/" + channelName;

describe(__filename, function () {
    beforeAll(async () => {
        const channel = await createChannel(channelName, null, 'websocket testing');
        if (getProp('statusCode', channel) === 201) {
            createdChannel = true;
            console.log(`created channel for ${__filename}`);
        }
    });

    var startingItem;

    it('posts item to channel', function (done) {
        if (!createdChannel) return done.fail('channel not created in before block');
        utils.postItemQ(channelResource)
            .then(function (result) {
                const location = fromObjectPath(['response', 'headers', 'location'], result);
                var itemURL = location;
                console.log('posted:', itemURL);
                startingItem = itemURL;
                done();
            });
    });

    var wsURL;

    it('builds websocket url', function () {
        if (!createdChannel) return fail('channel not created in before block');
        expect(startingItem).toBeDefined();
        var itemPathComponents = (startingItem || '').split('/');
        var itemYear = itemPathComponents[5];
        var itemMonth = itemPathComponents[6];
        var itemDay = itemPathComponents[7];
        var dayURL = channelResource + '/' + itemYear + '/' + itemMonth + '/' + itemDay;
        wsURL = dayURL.replace('http', 'ws') + '/ws';
    });

    var webSocket;
    var receivedMessages = [];

    it('opens websocket', function (done) {
        expect(wsURL).toBeDefined();
        if (!createdChannel) return done.fail('channel not created in before block');

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
        if (!createdChannel) return done.fail('channel not created in before block');
        utils.postItemQ(channelResource)
            .then(function (result) {
                const location = fromObjectPath(['response', 'headers', 'location'], result);
                var itemURL = location;
                console.log('posted:', itemURL);
                postedItem = itemURL;
                done();
            });
    });

    it('waits for data', function (done) {
        if (!createdChannel) return done.fail('channel not created in before block');
        var sentItems = [startingItem, postedItem];
        utils.waitForData(receivedMessages, sentItems, done);
    });

    it('verifies the correct data was received', function () {
        if (!createdChannel) return fail('channel not created in before block');
        expect(receivedMessages.length).toEqual(2);
        expect(receivedMessages).toContain(startingItem);
        expect(receivedMessages).toContain(postedItem);
    });

    it('closes websocket', function (done) {
        if (!createdChannel) return done.fail('channel not created in before block');
        webSocket.onclose = function () {
            console.log('closed:', wsURL);
            done();
        };

        webSocket.close();
    });
});
