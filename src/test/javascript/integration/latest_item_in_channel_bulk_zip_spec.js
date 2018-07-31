require('../integration_config');
const request = require('request');
const {
    fromObjectPath,
    getProp,
    hubClientPut,
} = require('../lib/helpers');

const channelName = utils.randomChannelName();
const channelResource = `${channelUrl}/${channelName}`;
const headers = { 'Content-Type': 'application/json' };

/**
 * create a channel
 * post two items
 * stream both items back with batch
 */
describe(__filename, function () {
    beforeAll(async () => {
        const response = await hubClientPut(channelResource, headers, { name: channelName, ttlDays: 1 });
        expect(getProp('statusCode', response)).toEqual(201);
    });

    utils.addItem(channelResource, 201);

    it('posts item', function (done) {
        utils.postItemQ(channelResource)
            .then(function (value) {
                const location = fromObjectPath(['response', 'headers', 'location'], value);
                console.log('location: ', location);
                done();
            });
    });

    it("gets zip items ", function (done) {
        request.get({
            url: channelResource + '/latest/10?stable=false&batch=true',
            followRedirect: false,
            headers: { Accept: "application/zip" },
        },
        function (err, response, body) {
            expect(err).toBeNull();
            expect(getProp('statusCode', response)).toBe(200);
            // todo - gfm - 8/19/15 - parse zip
            console.log("headers", getProp('headers', response));
            console.log("body", getProp('body', response));
            done();
        });
    });
});
