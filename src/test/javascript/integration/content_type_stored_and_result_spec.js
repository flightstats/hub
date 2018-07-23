require('../integration_config');
const {
    fromObjectPath,
    getProp,
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

    it('inserts an item', function (done) {
        var url = channelResource;
        var headers = {'Content-Type': 'application/fractals'};
        var body = messageText;

        utils.httpPost(url, headers, body)
            .then(function (response) {
                expect(getProp('statusCode', response)).toEqual(201);
                const contentType = fromObjectPath(['headers', 'content-type'], response);
                expect(contentType).toEqual('application/json');
                const links = fromObjectPath(['body', '_links'], response) || {};
                const { channel = {}, self = {} } = links;
                expect(channel.href).toEqual(channelResource);
                itemURL = self.href;
            })
            .finally(done);
    });

    it('verifies the correct content-type is returned', function (done) {
        if (!itemURL) return done.fail('itemURL failed initialization in previous test');
        utils.httpGet(itemURL)
            .then(function (response) {
                expect(getProp('statusCode', response)).toEqual(200);
                const contentType = fromObjectPath(['headers', 'content-type'], response);
                expect(contentType).toEqual('application/fractals');
                expect(getProp('body', response)).toContain(messageText);
            })
            .finally(done);
    });

});
