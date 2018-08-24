const {
    fromObjectPath,
    getProp,
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
const headers = { 'Content-Type': 'application/json' };

describe(__filename, function () {
    beforeAll(async () => {
        const body = { 'name': channelName };
        const response = await hubClientPost(channelUrl, headers, body);
        if (getProp('statusCode', response) === 201) {
            console.log(`successfully created channel for ${__filename}`);
        }
    });

    it('fetches the list of channels', async () => {
        const response = await hubClientGet(channelUrl, headers);
        expect(getProp('statusCode', response)).toEqual(200);
        const contentType = fromObjectPath(['headers', 'content-type'], response);
        const links = fromObjectPath(['body', '_links'], response) || {};
        const { channels = [], self = {} } = links;
        expect(contentType).toEqual('application/json');
        expect(self.href).toEqual(channelUrl);
        const channelURLs = channels.map(function (obj) {
            return getProp('href', obj) || '';
        });
        expect(channelURLs).toContain(channelResource);
    });
});
