require('../integration_config');
const {
    fromObjectPath,
    getProp,
    hubClientGet,
} = require('../lib/helpers');

const channelName = utils.randomChannelName();
const channelResource = `${channelUrl}/${channelName}`;
const headers = { 'Content-Type': 'application/json' };

describe(__filename, function () {
    it('creates a channel', function (done) {
        const body = { 'name': channelName };

        utils.httpPost(channelUrl, headers, body)
            .then(function (response) {
                expect(getProp('statusCode', response)).toEqual(201);
            })
            .finally(done);
    });

    it('fetches the list of channels', async () => {
        const response = await hubClientGet(channelUrl, headers);
        expect(getProp('statusCode', response)).toEqual(200);
        const contentType = fromObjectPath(['headers', 'content-type'], response);
        const links = fromObjectPath(['body', '_links'], response) || {};
        const { channels = [], self = {} } = links;
        expect(contentType).toEqual('application/json');
        expect(self.href).toEqual(channelUrl);
        const channelURLs = channels.map(function (obj) {
            return getProp('href', obj) || '';
        });
        expect(channelURLs).toContain(channelResource);
    });
});
