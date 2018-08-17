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
let createdChannel = false;
let startingItem = null;
let wsURL = null;
let webSocket = null;
const receivedMessages = [];
let postedItem = null;

describe(__filename, function () {
    beforeAll(async () => {
        const channel = await createChannel(channelName, null, 'websocket testing');
        if (getProp('statusCode', channel) === 201) {
            createdChannel = true;
            console.log(`created channel for ${__filename}`);
        }
    });

    it('posts item to channel', async () => {
        if (!createdChannel) return fail('channel not created in before block');
        const response = await hubClientPostTestItem(channelResource);
        startingItem = fromObjectPath(['headers', 'location'], response);
    });

    it('builds websocket url', function () {
        if (!createdChannel) return fail('channel not created in before block');
        expect(startingItem).toBeDefined();
        wsURL = (startingItem || '').replace('http', 'ws') + '/ws';
    });

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

    it('posts item to channel', async () => {
        if (!createdChannel) return fail('channel not created in before block');
        const response = await hubClientPostTestItem(channelResource);
        postedItem = fromObjectPath(['headers', 'location'], response);
        const condition = () => (receivedMessages.length === 1);
        await waitForCondition(condition);
    });

    it('verifies the correct data was received', function () {
        if (!createdChannel) return fail('channel not created in before block');
        expect(receivedMessages.length).toEqual(1);
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
