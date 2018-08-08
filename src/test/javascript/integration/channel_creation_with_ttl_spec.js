require('../integration_config');
const {
    fromObjectPath,
    getProp,
    hubClientGet,
} = require('../lib/helpers');
const channelName = utils.randomChannelName();
const channelResource = channelUrl + "/" + channelName;
const headers = { 'Content-Type': 'application/json' };
describe(__filename, function () {
    it('verifies the channel doesn\'t exist yet', async () => {
        const response = await hubClientGet(channelResource);
        expect(getProp('statusCode', response)).toEqual(404);
    });

    it('creates a channel with a valid TTL', function (done) {
        var url = channelUrl;
        var body = {'name': channelName, 'ttlMillis': 30000};

        utils.httpPost(url, headers, body)
            .then(function (response) {
                expect(getProp('statusCode', response)).toEqual(201);
                const contentType = fromObjectPath(['headers', 'content-type'], response);
                const ttlDays = fromObjectPath(['body', 'ttlDays'], response);
                expect(contentType).toEqual('application/json');
                expect(ttlDays).toEqual(120);
            })
            .finally(done);
    });

    it('verifies the channel does exist', async () => {
        const response = await hubClientGet(channelResource, headers);
        expect(getProp('statusCode', response)).toEqual(200);
        const contentType = fromObjectPath(['headers', 'content-type'], response);
        expect(contentType).toEqual('application/json');
        const ttlDays = fromObjectPath(['body', 'ttlDays'], response);
        const name = fromObjectPath(['body', 'name'], response);
        expect(name).toEqual(channelName);
        expect(ttlDays).toEqual(120);
    });
});
