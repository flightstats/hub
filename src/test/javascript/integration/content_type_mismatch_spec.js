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
                const links = fromObjectPath(['body', '_links'], response) || {};
                const { channel = {}, self = {} } = links;
                expect(contentType).toEqual('application/json');
                expect(channel.href).toEqual(channelResource);
                itemURL = self.href;
            })
            .finally(done);
    });

    it('verifies an error is returned when content-type doesn\'t match the accept header', function (done) {
        if (!itemURL) return done.fail('itemURL failed initialization in previous test');
        var url = itemURL;
        var headers = {'Accept': 'application/json'};

        utils.httpGet(url, headers)
            .then(function (response) {
                expect(getProp('statusCode', response)).toEqual(406);
            })
            .finally(done);
    });

});
