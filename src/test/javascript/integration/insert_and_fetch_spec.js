require('../integration_config');
const {
    fromObjectPath,
    getProp,
    hubClientGet,
    hubClientPost,
} = require('../lib/helpers');

const channelName = utils.randomChannelName();
const channelResource = `${channelUrl}/${channelName}`;
const messageText = `MY SUPER TEST CASE: this & <that>.${Math.random()}`;
let itemURL = null;
let itemResponse = {};

describe(__filename, function () {
    beforeAll(async () => {
        const body = { 'name': channelName };
        const contentJSON = { 'Content-Type': 'application/json' };
        const response = await hubClientPost(channelUrl, contentJSON, body);
        if (getProp('statusCode', response) === 201) {
            const headers = { 'Content-Type': 'text/plain' };
            itemResponse = await hubClientPost(channelResource, headers, messageText);
            itemURL = fromObjectPath(['body', '_links', 'self', 'href'], itemResponse);
        }
    });

    it('inserts an item', () => {
        expect(getProp('statusCode', itemResponse)).toEqual(201);
        const contentType = fromObjectPath(['headers', 'content-type'], itemResponse);
        const channelLink = fromObjectPath(['body', '_links', 'channel', 'href'], itemResponse);
        expect(contentType).toEqual('application/json');
        expect(channelLink).toEqual(channelResource);
    });

    it('verifies the item was inserted successfully', async () => {
        if (!itemURL) return fail('itemURL not defined in beforeAll block');
        const response = await hubClientGet(itemURL);
        expect(getProp('statusCode', response)).toEqual(200);
        const contentType = fromObjectPath(['headers', 'content-type'], response);
        const user = fromObjectPath(['headers', 'user'], response);
        expect(contentType).toEqual('text/plain');
        expect(user).toBeUndefined();
        expect(getProp('body', response)).toContain(messageText);
    });
});
