require('../integration_config');
const {
    hubClientGet,
    fromObjectPath,
    getProp,
} = require('../lib/helpers');

var channelName = utils.randomChannelName();
const channelResource = `${channelUrl}/${channelName}`;

describe(__filename, function () {
    it('verifies the channel doesn\'t exist yet', async () => {
        const response = await hubClientGet(channelResource);
        expect(getProp('statusCode', response)).toEqual(404);
    });

    it('creates the channel', function (done) {
        var url = channelUrl;
        var headers = {'Content-Type': 'application/json'};
        var body = {'name': channelName, 'ttlMillis': null};

        utils.httpPost(url, headers, body)
            .then(function (response) {
                expect(getProp('statusCode', response)).toEqual(201);
            })
            .finally(done);
    });

    it('updates the channel TTL', function (done) {
        var url = channelResource;
        var headers = {'Content-Type': 'application/json'};
        var body = {'ttlMillis': null};

        utils.httpPatch(url, headers, body)
            .then(function (response) {
                expect(getProp('statusCode', response)).toEqual(200);
                const contentType = fromObjectPath(['headers', 'content-type'], response);
                const name = fromObjectPath(['body', 'name'], response);
                expect(contentType).toEqual('application/json');
                expect(name).toEqual(channelName);
            })
            .finally(done);
    });
});
