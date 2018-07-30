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

    it('creates a channel with a replicationSource', function (done) {
        var url = channelUrl;
        var body = {'name': channelName, 'replicationSource': 'http://hub/channel/nada'};

        utils.httpPost(url, headers, body)
            .then(function (response) {
                const contentType = fromObjectPath(['headers', 'content-type'], response);
                const replicationSource = fromObjectPath(['body', 'replicationSource'], response);
                expect(getProp('statusCode', response)).toEqual(201);
                expect(contentType).toEqual('application/json');
                expect(replicationSource).toEqual('http://hub/channel/nada');
            })
            .finally(done);
    });

    it('verifies the channel does exist', async () => {
        const response = await hubClientGet(channelResource, headers);
        const contentType = fromObjectPath(['headers', 'content-type'], response);
        const replicationSource = fromObjectPath(['body', 'replicationSource'], response);
        const name = fromObjectPath(['body', 'name'], response);
        expect(getProp('statusCode', response)).toEqual(200);
        expect(contentType).toEqual('application/json');
        expect(name).toEqual(channelName);
        expect(replicationSource).toEqual('http://hub/channel/nada');
    });
});
