require('../integration_config');
const {
    hubClientGet,
    fromObjectPath,
    getProp,
} = require('../lib/helpers');

var channelName = utils.randomChannelName();
var channelResource = channelUrl + "/" + channelName;
var messageText = "Testing that the Content-Encoding header is returned";

describe(__filename, function () {
    it('creates a channel', function (done) {
        var url = channelUrl;
        var headers = {'Content-Type': 'application/json'};
        var body = {'name': channelName};

        utils.httpPost(url, headers, body)
            .then(function (response) {
                expect(getProp('statusCode', response)).toEqual(201);
            })
            .finally(done);
    });

    it('inserts an item', function (done) {
        var url = channelResource;
        var headers = {};
        var body = messageText;

        utils.httpPost(url, headers, body)
            .then(function (response) {
                expect(getProp('statusCode', response)).toEqual(201);
            })
            .finally(done);
    });

    it('verifies the Content-Encoding header is returned', async () => {
        const headers = { 'accept-encoding': 'gzip' };
        const response = await hubClientGet(channelResource, headers);
        console.log('response', response);
        const contentEncoding = fromObjectPath(['headers', 'content-encoding'], response);
        expect(contentEncoding).toEqual('gzip');
    });
});
