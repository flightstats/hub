const {
    fromObjectPath,
    getProp,
    hubClientDelete,
    hubClientGet,
    hubClientPost,
    randomChannelName,
} = require('../lib/helpers');
const {
    getChannelUrl,
} = require('../lib/config');

const channelUrl = getChannelUrl();
const channelName = randomChannelName();
const channelResource = `${channelUrl}/${channelName}`;
let itemURL = null;
let imageData = '';

describe(__filename, function () {
    beforeAll(async () => {
        const body = { 'name': channelName };
        const contentJSON = { 'Content-Type': 'application/json' };
        await hubClientPost(channelUrl, contentJSON, body);
    });

    it('downloads an image of a cat', async () => {
        const url = 'http://www.lolcats.com/images/u/08/32/lolcatsdotcombkf8azsotkiwu8z2.jpg';
        const headers = {};
        const isBinary = true;

        const response = await hubClientGet(url, headers, isBinary);
        expect(getProp('statusCode', response)).toEqual(200);
        imageData = getProp('body', response) || '';
    });

    it('inserts an image into the channel', async () => {
        const headers = { 'Content-Type': 'image/jpeg' };
        const body = Buffer.from(imageData, 'binary');

        const response = await hubClientPost(channelResource, headers, body);
        expect(getProp('statusCode', response)).toEqual(201);
        const channelLink = fromObjectPath(['body', '_links', 'channel', 'href'], response);
        expect(channelLink).toEqual(channelResource);
        itemURL = fromObjectPath(['body', '_links', 'self', 'href'], response);
    });

    it('verifies the image data was inserted correctly', async () => {
        if (!itemURL) return fail('itemURL is not defined by previous test');
        const headers = {};
        const isBinary = true;

        const response = await hubClientGet(itemURL, headers, isBinary);
        const responseBody = getProp('body', response) || '';
        expect(getProp('statusCode', response)).toEqual(200);
        expect(responseBody.length).toEqual(imageData.length);
    });

    afterAll(async () => {
        await hubClientDelete(channelResource);
    });
});
