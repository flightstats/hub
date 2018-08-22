require('../integration_config');
const {
    fromObjectPath,
    getProp,
    hubClientGet,
    hubClientPost,
} = require('../lib/helpers');
const {
    getChannelUrl,
} = require('../lib/config');

const channelUrl = getChannelUrl();
const channelName = utils.randomChannelName();
const channelResource = `${channelUrl}/${channelName}`;
const messageText = "there's a snake in my boot!";
let itemURL = null;
let itemResponse = {};

describe(__filename, function () {
    beforeAll(async () => {
        const contentJSON = { 'Content-Type': 'application/json' };
        const body = { 'name': channelName };
        const response = await hubClientPost(channelUrl, contentJSON, body);
        if (getProp('statusCode', response) === 201) {
            const headers = { 'Content-Type': 'text/plain' };
            itemResponse = await hubClientPost(channelResource, headers, messageText);
            itemURL = fromObjectPath(['body', '_links', 'self', 'href'], itemResponse);
        }
    });

    it('inserted an item', async () => {
        expect(getProp('statusCode', itemResponse)).toEqual(201);
    });

    it('verifies the creation-date header is returned', async () => {
        if (!itemURL) return fail('itemURL failed initialization in previous test');
        const response = await hubClientGet(itemURL);
        expect(getProp('statusCode', response)).toEqual(200);
        const creationDate = fromObjectPath(['headers', 'creation-date'], response);
        expect(creationDate).toContain('T');
    });
});
