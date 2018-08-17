require('../integration_config');
const WebSocket = require('ws');
const {
    createChannel,
    fromObjectPath,
    getProp,
    hubClientPostTestItem,
    waitForCondition,
} = require('../lib/helpers');
const { getChannelUrl } = require('../lib/config');

const channelUrl = getChannelUrl();
const channelName = utils.randomChannelName();
const channelResource = `${channelUrl}/${channelName}`;
const itemURLs = [];
let createdChannel = false;
let webSocket;
const wsURL = `${channelResource.replace('http', 'ws')}/ws`;
const receivedMessages = [];

describe(__filename, function () {
    beforeAll(async () => {
        const channel = await createChannel(channelName, null, 'websocket testing');
        if (getProp('statusCode', channel) === 201) {
            createdChannel = true;
            console.log(`created channel for ${__filename}`);
        }
    });

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

    it('posts item to channel', async () => {
        if (!createdChannel) return fail('channel not created in before block');
        const response = await hubClientPostTestItem(channelResource);
        const location = fromObjectPath(['headers', 'location'], response);
        console.log('posted:', location);
        itemURLs.push(location);
        const condition = () => (receivedMessages.length === itemURLs.length);
        await waitForCondition(condition);
    });

    it('verifies the correct data was received', function () {
        if (!createdChannel) return fail('channel not created in before block');
        expect(receivedMessages.length).toEqual(itemURLs.length);
        for (let i = 0; i < itemURLs.length; i++) {
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
