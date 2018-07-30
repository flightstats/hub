require('../integration_config');
const {
    fromObjectPath,
    getProp,
    hubClientGet,
} = require('../lib/helpers');

const channelName = utils.randomChannelName();
const channelResource = `${channelUrl}/${channelName}`;
const messageText = `MY SUPER TEST CASE: this & <that>.${Math.random()}`;

describe(__filename, function () {
    it('creates a channel', function (done) {
        const url = channelUrl;
        const body = { 'name': channelName };
        const headers = { 'Content-Type': 'application/json' };
        utils.httpPost(url, headers, body)
            .then(function (response) {
                expect(getProp('statusCode', response)).toEqual(201);
            })
            .finally(done);
    });

    let itemURL;

    it('inserts an item', function (done) {
        const url = channelResource;
        const headers = { 'Content-Type': 'text/plain' };
        const body = messageText;

        utils.httpPost(url, headers, body)
            .then(function (response) {
                expect(getProp('statusCode', response)).toEqual(201);
                const contentType = fromObjectPath(['headers', 'content-type'], response);
                const links = fromObjectPath(['body', '_links'], response) || {};
                const { channel = {}, self = {} } = links;
                expect(contentType).toEqual('application/json');
                expect(channel.href).toEqual(channelResource);
                itemURL = self.href;
            })
            .finally(done);
    });

    it('verifies the item was inserted successfully', async () => {
        const response = await hubClientGet(itemURL);
        expect(getProp('statusCode', response)).toEqual(200);
        const contentType = fromObjectPath(['headers', 'content-type'], response);
        const user = fromObjectPath(['headers', 'user'], response);
        expect(contentType).toEqual('text/plain');
        expect(user).toBeUndefined();
        expect(getProp('body', response)).toContain(messageText);
    });
});
