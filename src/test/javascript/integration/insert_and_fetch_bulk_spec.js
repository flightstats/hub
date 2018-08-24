const {
    fromObjectPath,
    getProp,
    hubClientPut,
    parseJson,
    randomChannelName,
} = require('../lib/helpers');
const {
    getChannelUrl,
} = require('../lib/config');

const channelUrl = getChannelUrl();
const request = require('request');
const channelName = randomChannelName();
const channelResource = `${channelUrl}/${channelName}/bulk`;
const headers = { 'Content-Type': 'application/json' };
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
let items = [];

describe(__filename, function () {
    beforeAll(async () => {
        const channelBody = { ttlDays: 1, tags: ['bulk'] };
        const response = await hubClientPut(`${channelUrl}/${channelName}`, headers, channelBody);
        expect(getProp('statusCode', response)).toEqual(201);
    });

    it(`bulk items to ${channelResource}`, function (done) {
        request.post({
            url: channelResource,
            headers: {'Content-Type': "multipart/mixed; boundary=abcdefg"},
            body: multipart,
        },
        function (err, response, body) {
            expect(err).toBeNull();
            expect(getProp('statusCode', response)).toBe(201);
            const parse = parseJson(response, __filename);
            console.log(getProp('body', response));
            const uris = fromObjectPath(['_links', 'uris'], parse) || [];
            expect(uris.length).toBe(2);
            items = uris;
            done();
        });
    });

    it(`gets first item ${channelResource}`, function (done) {
        request.get({url: items[0]},
            function (err, response, body) {
                expect(err).toBeNull();
                const contentType = fromObjectPath(['headers', 'content-type'], response);
                expect(getProp('statusCode', response)).toBe(200);
                expect(getProp('body', response)).toBe('<coffee><roast>french</roast><coffee>');
                expect(contentType).toBe('application/xml');
                done();
            });
    });

    it(`gets second item ${channelResource}`, function (done) {
        request.get({url: items[1]},
            function (err, response, body) {
                expect(err).toBeNull();
                const contentType = fromObjectPath(['headers', 'content-type'], response);
                expect(getProp('statusCode', response)).toBe(200);
                expect(getProp('body', response)).toBe('{ "type" : "coffee", "roast" : "french" }');
                expect(contentType).toBe('application/json');
                done();
            });
    });

    it(`calls previous ${channelResource}`, function (done) {
        request.get({url: items[1] + '/previous?trace=true&stable=false', followRedirect: false},
            function (err, response, body) {
                expect(err).toBeNull();
                const location = fromObjectPath(['headers', 'location'], response);
                // console.log('response', response);
                expect(getProp('statusCode', response)).toBe(303);
                expect(location).toBe(items[0]);
                done();
            });
    });

    it(`calls next ${channelResource}`, function (done) {
        request.get({url: items[0] + '/next?trace=true&stable=false', followRedirect: false},
            function (err, response, body) {
                expect(err).toBeNull();
                console.log('body', body);
                const location = fromObjectPath(['headers', 'location'], response);
                expect(getProp('statusCode', response)).toBe(303);
                expect(location).toBe(items[1]);
                done();
            });
    });
});
