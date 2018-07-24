require('../integration_config');
const {
    fromObjectPath,
    getProp,
    hubClientGet,
} = require('../lib/helpers');

var channelName = utils.randomChannelName();
var channelResource = channelUrl + "/" + channelName;
var messageText = "MY SUPER TEST CASE: this & <that>. " + Math.random().toString();

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

    var itemURL;

    it('inserts and item', function (done) {
        var url = channelResource;
        var headers = {};
        var body = messageText;

        utils.httpPost(url, headers, body)
            .then(function (response) {
                expect(getProp('statusCode', response)).toEqual(201);
                const links = fromObjectPath(['body', '_links'], response) || {};
                const { channel = {}, self = {} } = links;
                expect(channel.href).toEqual(channelResource);
                /*
                  TODO: I think that setting a variable in an it block of a test to use
                  in the next test could possibly be better handled in a before block
                  leaving all setup in one place and all assertions in the "it"s
                */
                itemURL = self.href;
            })
            .finally(done);
    });

    it('verifies the correct Content-Type header is returned', async () => {
        if (!itemURL) return fail('itemURL failed initialization in previous test');
        const response = await hubClientGet(itemURL);
        const contentType = fromObjectPath(['headers', 'content-type'], response);
        expect(getProp('status', response)).toEqual(200);
        expect(contentType).toEqual('application/octet-stream');
        expect(getProp('body', response)).toContain(messageText);
    });
});
