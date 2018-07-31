require('../integration_config');
const {
    fromObjectPath,
    getProp,
    hubClientPut,
} = require('../lib/helpers');
const channelName = utils.randomChannelName();
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

    utils.itSleeps(1000);

    it(`fails with 403 posting item to ${channelName} replicated channel`, function (done) {
        request.post({
            url: channelResource,
            headers: { "Content-Type": "application/json", user: 'somebody' },
            body: JSON.stringify({ "data": Date.now() }),
        },
        function (err, response, body) {
            expect(err).toBeNull();

            // console.log('body', body);
            const contentType = fromObjectPath(['headers', 'content-type'], response);
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
});
