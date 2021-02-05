const {
    fromObjectPath,
    getProp,
    hubClientDelete,
    hubClientPut,
    hubClientPostTestItem,
    parseJson,
    randomChannelName,
} = require('../lib/helpers');
const {
    getChannelUrl,
} = require('../lib/config');

const channelUrl = getChannelUrl();
const headers = { 'Content-Type': 'application/json' };
const request = require('request');
const channelName = randomChannelName();
const channelResource = `${channelUrl}/${channelName}`;
let channelCreated = false;
/**
 * create a channel
 * post an item
 * does not get the item back out with earliest - stable
 * get the item back out with earliest - unstable
 */
let posted = null;
describe(__filename, function () {
    beforeAll(async () => {
        const response = await hubClientPut(channelResource, headers, {"name": channelName, "ttlDays": 1});
        if (getProp('statusCode', response) === 201) {
            channelCreated = true;
        }
    });

    it('posts item', async () => {
        if (!channelCreated) return fail('channel not created in before block');
        const response = await hubClientPostTestItem(channelResource);
        expect(getProp('statusCode', response)).toEqual(201);
        posted = response.header('location');;
    });

    it('posts another item', async () => {
        if (!channelCreated) return fail('channel not created in before block');
        const response = await hubClientPostTestItem(channelResource);
        expect(getProp('statusCode', response)).toEqual(201);
    });

    it("gets earliest stable in channel ", function (done) {
        if (!channelCreated) return done.fail('channel not created in before block');
        request.get({url: channelResource + '/earliest', followRedirect: false},
            function (err, response, body) {
                expect(err).toBeNull();
                expect(getProp('statusCode', response)).toBe(404);
                done();
            });
    });

    it("gets earliest unstable in channel ", function (done) {
        if (!channelCreated) return done.fail('channel not created in before block');
        request.get({url: channelResource + '/earliest?stable=false', followRedirect: false},
            function (err, response, body) {
                expect(err).toBeNull();
                expect(getProp('statusCode', response)).toBe(303);
                const location = response.header('location');;
                expect(location).toBeDefined();
                expect(posted).toBeDefined();
                expect(location).toBe(posted);
                done();
            });
    });

    it("gets earliest N unstable in channel ", function (done) {
        if (!channelCreated) return done.fail('channel not created in before block');
        request.get({url: channelResource + '/earliest/10?stable=false', followRedirect: false},
            function (err, response, body) {
                expect(err).toBeNull();
                expect(getProp('statusCode', response)).toBe(200);
                const parsed = parseJson(response, __filename);
                const links = getProp('_links', parsed);
                if (links) {
                    const { next, previous, uris = [] } = links;
                    expect(uris.length).toBe(2);
                    expect(posted).toBeDefined();
                    expect(uris[0]).toBe(posted);
                    expect(next).toBeDefined();
                    expect(previous).not.toBeDefined();
                } else {
                    expect(links).toBe(true);
                }
                done();
            });
    });

    afterAll(async () => {
        await hubClientDelete(channelResource);
    });
});
