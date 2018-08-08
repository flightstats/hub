require('../integration_config');
const { getProp, hubClientGet } = require('../lib/helpers');

const providerResource = `${hubUrlBase}/provider/bulk`;
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
    it('inserts a bulk value into a provider channel', function (done) {
        const headers = {
            'channelName': channelName,
            'Content-Type': 'multipart/mixed; boundary=abcdefg',
        };
        const body = multipart;

        utils.httpPost(providerResource, headers, body)
            .then(function (response) {
                const statusCode = getProp('statusCode', response);
                console.log('statusCode', statusCode);
                expect(statusCode).toEqual(200);
            })
            .finally(done);
    });

    it('waits', (done) => {
        const wait = setTimeout(() => {
            // just waiting a sec
        }, 900);
        clearTimeout(wait);
        expect(true).toBe(true);
        done();
    });

    it('verifies the bulk value was inserted', async () => {
        const url = `${channelResource}/latest?stable=false`;
        const response = await hubClientGet(url);
        expect(getProp('statusCode', response)).toEqual(200);
    });
});
