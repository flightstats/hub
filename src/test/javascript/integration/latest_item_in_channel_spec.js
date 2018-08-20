require('../integration_config');
const request = require('request');
const {
    fromObjectPath,
    getProp,
    hubClientPut,
    hubClientPostTestItem,
    itSleeps,
} = require('../lib/helpers');

const channelName = utils.randomChannelName();
const channelResource = `${channelUrl}/${channelName}`;
const headers = { 'Content-Type': 'application/json' };
let posted = null;
/**
 * create a channel
 * post an item
 * does not get the item back out with latest - stable
 * get the item back out with latest - unstable
 */
describe(__filename, function () {
    beforeAll(async () => {
        const response = await hubClientPut(channelResource, headers, { name: channelName, ttlDays: 1 });
        expect(getProp('statusCode', response)).toEqual(201);
    });

    it('posts items to the channel', async () => {
        const response1 = await hubClientPostTestItem(channelResource);
        expect(getProp('statusCode', response1)).toEqual(201);

        const response2 = await hubClientPostTestItem(channelResource);
        expect(getProp('statusCode', response2)).toEqual(201);
        posted = fromObjectPath(['headers', 'location'], response2);
    });

    it("gets latest stable in channel ", function (done) {
        request.get({url: `${channelResource}/latest`, followRedirect: false},
            function (err, response, body) {
                expect(err).toBeNull();
                expect(getProp('statusCode', response)).toBe(404);
                done();
            });
    });

    it("gets latest unstable in channel ", function (done) {
        request.get({url: `${channelResource}/latest?stable=false`, followRedirect: false},
            function (err, response, body) {
                expect(err).toBeNull();
                expect(getProp('statusCode', response)).toBe(303);
                const location = fromObjectPath(['headers', 'location'], response);
                expect(location).toBe(posted);
                done();
            });
    });

    it("gets latest N unstable in channel ", function (done) {
        request.get({url: `${channelResource}/latest/10?stable=false`, followRedirect: false},
            function (err, response, body) {
                expect(err).toBeNull();
                expect(getProp('statusCode', response)).toBe(200);
                const parsed = utils.parseJson(response, __filename);
                const uris = fromObjectPath(['_links', 'uris'], parsed) || [];
                expect(uris.length).toBe(2);
                expect(uris[1]).toBe(posted);
                done();
            });
    });

    it('waits 6000 ms', async () => {
        await itSleeps(6000);
    });
    it('posts another item to the channel', async () => {
        const response = await hubClientPostTestItem(channelResource);
        expect(getProp('statusCode', response)).toEqual(201);
    });

    it("gets latest stable in channel ", function (done) {
        request.get({url: `${channelResource}/latest?stable=true&trace=true`, followRedirect: false},
            function (err, response, body) {
                expect(err).toBeNull();
                expect(getProp('statusCode', response)).toBe(303);
                const location = fromObjectPath(['headers', 'location'], response);
                expect(location).toBe(posted);
                done();
            });
    });
});
