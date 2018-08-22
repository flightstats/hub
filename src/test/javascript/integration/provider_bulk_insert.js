require('../integration_config');
const {
    followRedirectIfPresent,
    getProp,
    hubClientGet,
    hubClientPost,
} = require('../lib/helpers');
const {
    getChannelUrl,
    getHubUrlBase,
} = require('../lib/config');

const channelUrl = getChannelUrl();
const providerResource = `${getHubUrlBase()}/provider/bulk`;
const channelName = utils.randomChannelName();
const channelResource = `${channelUrl}/${channelName}`;
const multipart = [
    'This is a message with multiple parts in MIME format.  This section is ignored.\r\n',
    '--abcdefg\r\n',
    'Content-Type: application/xml\r\n',
    ' \r\n',
    '<coffee><roast>french</roast><coffee>\r\n',
    '--abcdefg\r\n',
    'Content-Type: application/json\r\n',
    ' \r\n',
    '{ "type" : "coffee", "roast" : "french" }\r\n',
    '--abcdefg--',
].join('');

describe(__filename, function () {
    it('inserts a bulk value into a provider channel', async () => {
        const headers = {
            'channelName': channelName,
            'Content-Type': 'multipart/mixed; boundary=abcdefg',
        };

        const response = await hubClientPost(providerResource, headers, multipart);
        expect(getProp('statusCode', response)).toEqual(200);
    });

    it('waits', async () => {
        const wait = () => new Promise((resolve) => {
            console.log('started');
            const timer = setTimeout(() => {
                clearTimeout(timer);
                console.log('waited');
                resolve(true);
                // just waiting a sec
            }, 2000);
        });
        await wait();
        expect(true).toBe(true);
    });

    it('verifies the bulk value was inserted', async () => {
        const url = `${channelResource}/latest?stable=false`;
        const headers = { 'Content-Type': 'application/json' };
        const response = await hubClientGet(url);
        const response2 = await followRedirectIfPresent(response, headers);
        expect(getProp('statusCode', response2)).toEqual(200);
    });
});
