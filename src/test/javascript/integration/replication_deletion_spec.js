const {
    fromObjectPath,
    getProp,
    hubClientDelete,
    hubClientPut,
    itSleeps,
    randomChannelName,
} = require('../lib/helpers');
const {
    getChannelUrl,
} = require('../lib/config');

const channelUrl = getChannelUrl();
const channelName = randomChannelName();
const channelResource = `${channelUrl}/${channelName}`;
const headers = { 'Content-Type': 'application/json' };
const request = require('request');

/**
 * 1 - Create local channel with replicationSource to non-existent channel
 * 2 - Delete local channel
 */
describe(__filename, function () {
    beforeAll(async () => {
        const response = await hubClientPut(channelResource, headers, { replicationSource: 'http://hub/channel/none' });
        expect(getProp('statusCode', response)).toEqual(201);
    });

    it('waits 1000 ms', async () => {
        await itSleeps(1000);
    });

    it(`fails with 403 posting item to ${channelName} replicated channel`, function (done) {
        request.post({
            url: channelResource,
            headers: { "Content-Type": "application/json", user: 'somebody' },
            body: JSON.stringify({ "data": Date.now() }),
        },
        function (err, response, body) {
            expect(err).toBeNull();

            // console.log('body', body);
            const contentType = response('content-type');
            expect(contentType).toBe('application/json');
            expect(getProp('statusCode', response)).toBe(403);
            expect(body).toBe(`${channelName} cannot modified while replicating`);
            done();
        });
    });

    it('deletes the local channel', function (done) {
        request.del({ url: channelResource },
            function (err, response, body) {
                expect(err).toBeNull();
                expect(getProp('statusCode', response)).toBe(202);
                done();
            });
    });

    afterAll(async () => {
        await hubClientDelete(channelResource);
    });
});
