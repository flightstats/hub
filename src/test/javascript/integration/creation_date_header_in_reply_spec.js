require('../integration_config');
const {
    fromObjectPath,
    getProp,
} = require('../lib/helpers');

var channelName = utils.randomChannelName();
var channelResource = channelUrl + "/" + channelName;
var messageText = "there's a snake in my boot!";

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
        var headers = {'Content-Type': 'text/plain'};
        var body = messageText;

        utils.httpPost(url, headers, body)
            .then(function (response) {
                expect(getProp('statusCode', response)).toEqual(201);
                const selfLink = fromObjectPath(['body', '_links', 'self', 'href'], response);
                itemURL = selfLink;
            })
            .finally(done);
    });

    it('verifies the creation-date header is returned', function (done) {
        if (!itemURL) return done.fail('itemURL failed initialization in previous test');
        utils.httpGet(itemURL)
            .then(function (response) {
                expect(getProp('statusCode', response)).toEqual(200);
                const creationDate = fromObjectPath(['headers', 'creation-date'], response);
                expect(creationDate).toContain('T');
            })
            .finally(done);
    });

});
