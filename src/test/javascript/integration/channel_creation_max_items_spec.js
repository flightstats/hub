require('../integration_config');
const {
    fromObjectPath,
    getProp,
    hubClientGet,
} = require('../lib/helpers');

var channelName = utils.randomChannelName();
var channelResource = channelUrl + "/" + channelName;

describe(__filename, function () {
    it('verifies the channel doesn\'t exist yet', async () => {
        const response = await hubClientGet(channelResource);
        expect(getProp('status', response)).toEqual(404);
    });

    it('creates a channel with maxItems set', function (done) {
        var maxItems = 50;
        var url = channelUrl;
        var headers = {'Content-Type': 'application/json'};
        var body = {'name': channelName, 'maxItems': maxItems};

        utils.httpPost(url, headers, body)
            .then(function (response) {
                const responseHeaders = getProp('headers', response);
                const responseBody = getProp('body', response);
                expect(getProp('statusCode', response)).toEqual(201);
                const [contentType, location] = ['content-type', 'location']
                    .map(key => getProp(key, responseHeaders));
                const selfLink = fromObjectPath(['_links', 'self', 'href'], responseBody);
                const [
                    name,
                    description,
                    replicationSource,
                ] = ['name', 'description', 'replicationSource']
                    .map(key => getProp(key, responseBody));
                expect(getProp('statusCode', response)).toEqual(201);
                expect(contentType).toEqual('application/json');
                expect(location).toEqual(channelResource);
                expect(selfLink).toEqual(channelResource);
                expect(name).toEqual(channelName);
                expect(maxItems).toEqual(maxItems);
                expect(description).toEqual('');
                expect(replicationSource).toEqual('');
            })
            .finally(done);
    });

    it('verifies the channel does exist', async () => {
        const response = await hubClientGet(channelResource);
        expect(getProp('status', response)).toEqual(200);
    });
});
