const request = require('request');
const {
    fromObjectPath,
    getProp,
    hubClientDelete,
    hubClientPut,
    randomChannelName,
} = require('../lib/helpers');
const {
    getChannelUrl,
} = require('../lib/config');

const channelName = randomChannelName();
const channelResource = `${getChannelUrl()}/${channelName}`;
let channelCreated = false;
const multipart = [
    'This is a message with multiple parts in MIME format.  This section is ignored.\r\n',
    '--abcdefg\r\n',
    'Content-Type: text/plain\r\n',
    ' \r\n',
    'message one\r\n',
    '--abcdefg\r\n',
    'Content-Type: text/plain\r\n',
    ' \r\n',
    'message two\r\n',
    '--abcdefg\r\n',
    'Content-Type: text/plain\r\n',
    ' \r\n',
    'message three\r\n',
    '--abcdefg\r\n',
    'Content-Type: text/plain\r\n',
    ' \r\n',
    'message four\r\n',
    '--abcdefg--',
].join('');
let location = '';
/**
 * create a channel
 * post 4 items as multipart to the single item endpoint.
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

    it("post multipart item to " + channelName, function (done) {
        if (!channelCreated) return done.fail('channel not created in before block');
        request.post({
            url: channelResource,
            headers: {'Content-Type': "multipart/mixed; boundary=abcdefg"},
            body: multipart,
        },
        function (err, response, body) {
            expect(err).toBeNull();
            expect(getProp('statusCode', response)).toBe(201);
            location = fromObjectPath(['headers', 'location'], response);
            console.log('location', location);
            expect(location).toBeDefined();
            console.log(getProp('body', response));
            done();
        });
    });

    it("verifies content " + channelName, function (done) {
        if (!channelCreated) return done.fail('channel not created in before block');
        request.get({url: location},
            function (err, response, body) {
                expect(err).toBeNull();
                expect(getProp('statusCode', response)).toBe(200);
                console.log('verifies content callback body', body);
                const headers = getProp('headers', response);
                console.log(headers);
                const contentType = getProp('content-type', headers);
                expect(contentType).toBe('multipart/mixed;boundary=abcdefg');
                expect(body).toBe(multipart);
                done();
            });
    });

    afterAll(async () => {
        await hubClientDelete(channelResource);
    });
});
