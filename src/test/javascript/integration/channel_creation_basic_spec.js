require('../integration_config');
const {
    fromObjectPath,
    getProp,
    hubClientGet,
} = require('../lib/helpers');

const channelName = utils.randomChannelName();
const channelResource = channelUrl + "/" + channelName;

describe(__filename, function () {
    it('verifies the channel doesn\'t exist', async () => {
        const response = await hubClientGet(channelResource);
        expect(getProp('statusCode', response)).toEqual(404);
    });

    it('creates the channel', function (done) {
        var uri = channelUrl;
        var headers = {'Content-Type': 'application/json'};
        var body = {'name': channelName};

        utils.httpPost(uri, headers, body)
            .then(function (response) {
                const headers = getProp('headers', response);
                const responseBody = getProp('body', response);
                expect(getProp('statusCode', response)).toEqual(201);
                const [contentType, location] = ['content-type', 'location']
                    .map(key => getProp(key, headers));
                const selfLink = fromObjectPath(['_links', 'self', 'href'], responseBody);
                const [
                    name,
                    ttlDays,
                    description,
                    replicationSource,
                    storage,
                ] = ['name', 'ttlDays', 'description', 'replicationSource', 'storage']
                    .map(key => getProp(key, responseBody));
                expect(contentType).toEqual('application/json');
                expect(location).toEqual(channelResource);
                expect(selfLink).toEqual(channelResource);
                expect(name).toEqual(channelName);
                expect(ttlDays).toEqual(120);
                expect(description).toEqual('');
                expect(replicationSource).toEqual('');
                expect(storage).toEqual('SINGLE');
            })
            .finally(done);
    });

    it('verifies the channel does exist', async () => {
        const response = await hubClientGet(channelResource);
        expect(getProp('statusCode', response)).toEqual(200);
    });
});
