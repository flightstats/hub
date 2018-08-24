const {
    getProp,
    hubClientPost,
    hubClientPut,
    randomChannelName,
} = require('../lib/helpers');
const {
    getChannelUrl,
} = require('../lib/config');

const channelName = randomChannelName();
const channelResource = `${getChannelUrl()}/${channelName}`;
const resourceUrl = `${channelResource}/bulk`;
const contentMultipart = { 'Content-Type': "multipart/mixed; boundary=abcdefg" };
let channelCreated = false;
/**
 * create a channel
 * post malformed bulk payload
 * get back an error
 */
describe(__filename, function () {
    beforeAll(async () => {
        const body = {
            name: channelName,
            ttlDays: 1,
            tags: ['bulk'],
        };
        const url = `${getChannelUrl()}/${channelName}`;
        const headers = { 'Content-Type': 'application/json' };
        const response = await hubClientPut(url, headers, body);
        if (getProp('statusCode', response) === 201) {
            channelCreated = true;
        }
    });

    it(`posts malformed item to ${channelResource} 1`, async () => {
        if (!channelCreated) return fail('channel not created in before block');
        const response = await hubClientPost(resourceUrl, contentMultipart, '--abcdefg--');
        expect(response instanceof Error).toBe(false);
        expect(getProp('statusCode', response)).toBe(400);
    });

    it(`posts malformed item to ${channelResource} 2`, async () => {
        if (!channelCreated) return fail('channel not created in before block');
        const response = await hubClientPost(resourceUrl, contentMultipart, '--abcdefg\r\n --abcdefg--');
        expect(response instanceof Error).toBe(false);
        expect(getProp('statusCode', response)).toBe(400);
    });
});
