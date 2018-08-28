const request = require('request');
const {
    createChannel,
    fromObjectPath,
    getProp,
    hubClientPostTestItem,
    hubClientDelete,
    randomChannelName,
} = require('../lib/helpers');
const { getChannelUrl } = require('../lib/config');

const channelUrl = getChannelUrl();
const channelName = randomChannelName();
const channelResource = `${channelUrl}/${channelName}`;
let createdChannel = false;
/**
 * This should:
 *
 * 1 - create a channel
 * 2 - post items into the channel
 * 3 - verify that records are returned via time query
 */
describe(__filename, function () {
    beforeAll(async () => {
        const channel = await createChannel(channelName);
        if (getProp('statusCode', channel) === 201) {
            createdChannel = true;
            console.log(`created channel for ${__filename}`);
        }
    });

    it('queries before insertion', function (done) {
        if (!createdChannel) return done.fail('channel not created in before block');
        request.get({url: channelResource + '/time/minute?stable=false', json: true},
            function (err, response, body) {
                expect(err).toBeNull();
                expect(getProp('statusCode', response)).toBe(200);
                const uris = fromObjectPath(['_links', 'uris'], body);
                const urisLength = !!uris && uris.length === 0;
                expect(urisLength).toBe(true);
                done();
            });
    });

    it('posts items to the channel', async () => {
        const response1 = await hubClientPostTestItem(channelResource);
        expect(getProp('statusCode', response1)).toEqual(201);

        const response2 = await hubClientPostTestItem(channelResource);
        expect(getProp('statusCode', response2)).toEqual(201);

        const response3 = await hubClientPostTestItem(channelResource);
        expect(getProp('statusCode', response3)).toEqual(201);
    });

    const callTime = (url, items, calls, done) => {
        if (!createdChannel) return done.fail('channel not created in before block');
        request.get({ url, json: true },
            function (err, response, body) {
                expect(err).toBeNull();
                expect(getProp('statusCode', response)).toBe(200);
                calls++;
                const uris = fromObjectPath(['_links', 'uris'], body);
                const prevLink = fromObjectPath(['_links', 'previous', 'href'], body);
                items = items.concat(uris);
                if (items.length === 4) {
                    done();
                } else if (calls < 10) {
                    callTime(prevLink, items, calls, done);
                } else {
                    done('unable to find 4 items in ' + calls + ' calls');
                }
            });
    };

    it('gets items from channel second', function (done) {
        callTime(channelResource + '/time/second?stable=false', [], 0, done);
    });

    it('gets items from channel minute', function (done) {
        callTime(channelResource + '/time/minute?stable=false', [], 0, done);
    });

    it('gets items from channel hour', function (done) {
        callTime(channelResource + '/time/hour?stable=false', [], 0, done);
    });

    it('gets items from channel day', function (done) {
        callTime(channelResource + '/time/day?stable=false', [], 0, done);
    });

    afterAll(async () => {
        await hubClientDelete(channelResource);
    });
});
