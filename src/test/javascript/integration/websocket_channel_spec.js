require('../integration_config');
const { createChannel, fromObjectPath, getProp } = require('../lib/helpers');
var WebSocket = require('ws');

var channelName = utils.randomChannelName();
const channelResource = `${channelUrl}/${channelName}`;
let createdChannel = false;

describe(__filename, function () {
    beforeAll(async () => {
        const channel = await createChannel(channelName, null, 'websocket testing');
        if (getProp('statusCode', channel) === 201) {
            createdChannel = true;
            console.log(`created channel for ${__filename}`);
        }
    });

    var webSocket;
    var wsURL = channelResource.replace('http', 'ws') + '/ws';
    var receivedMessages = [];

    it('opens websocket', function (done) {
        expect(wsURL).not.toEqual('undefined');
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

    var itemURLs = [];

    it('posts item to channel', function (done) {
        if (!createdChannel) return done.fail('channel not created in before block');
        utils.postItemQ(channelResource)
            .then(function (result) {
                const location = fromObjectPath(['response', 'headers', 'location'], result);
                console.log('posted:', location);
                itemURLs.push(location);
                done();
            });
    });

    it('waits for data', function (done) {
        if (!createdChannel) return done.fail('channel not created in before block');
        utils.waitForData(receivedMessages, itemURLs, done);
    });

    it('verifies the correct data was received', function () {
        if (!createdChannel) return fail('channel not created in before block');
        expect(receivedMessages.length).toEqual(itemURLs.length);
        for (var i = 0; i < itemURLs.length; ++i) {
            expect(receivedMessages).toContain(itemURLs[i]);
        }
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
