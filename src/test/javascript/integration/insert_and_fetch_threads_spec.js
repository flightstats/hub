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

const channelUrl = getChannelUrl();
const request = require('request');
const channelName = randomChannelName();
const channelResource = `${channelUrl}/${channelName}`;
const headers = { 'Content-Type': 'application/json' };
const MINUTE = 60 * 1000;
const channelBody = {
    tags: ["test"],
};
let location = null;
const SIZE = 41 * 1024;
/**
 * 1 - create a channel
 * 2 - post an item with threads
 * 3 - fetch the item and verify bytes
 */
describe(__filename, function () {
    beforeAll(async () => {
        const response = await hubClientPut(channelResource, headers, channelBody);
        expect(getProp('statusCode', response)).toEqual(201);
    });

    it("posts a large item to " + channelResource, function (done) {
        request.post({
            url: channelResource + '?threads=3',
            headers: {'Content-Type': "text/plain"},
            body: Array(SIZE).join("a"),
        },
        function (err, response, body) {
            expect(err).toBeNull();
            expect(getProp('statusCode', response)).toBe(201);
            console.log(getProp('body', response));
            const location = response.header('location');
            console.log(location);
            done();
        });
    }, 5 * MINUTE);

    it("gets item " + channelResource, function (done) {
        request.get({url: location},
            function (err, response, body) {
                expect(err).toBeNull();
                expect(getProp('statusCode', response)).toBe(200);
                const contentType = response('content-type');
                const responseBody = getProp('body', response) || '';
                expect(contentType).toBe('text/plain');
                expect(responseBody.length).toBe(SIZE - 1);
                done();
            });
    }, 5 * MINUTE);

    afterAll(async () => {
        await hubClientDelete(channelResource);
    });
});
