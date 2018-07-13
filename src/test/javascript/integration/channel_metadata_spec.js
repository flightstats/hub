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

    it('inserts an item into the channel', function (done) {
        var url = channelResource;
        var headers = {'Content-Type': 'text/plain'};
        var body = messageText;

        utils.httpPost(url, headers, body)
            .then(function (response) {
                expect(getProp('statusCode', response)).toEqual(201);
            })
            .finally(done);
    });

    it('verifies the channel metadata is accurate', function (done) {
        var url = channelResource + '/';

        utils.httpGet(url)
            .then(utils.followRedirectIfPresent)
            .then(function (response) {
                expect(getProp('statusCode', response)).toEqual(200);
                const contentType = fromObjectPath(['headers', 'content-type'], response);
                const latestLInk = fromObjectPath(['body', '_links', 'latest', 'href'], response);
                const name = fromObjectPath(['body', 'name'], response);
                const ttlDays = fromObjectPath(['body', 'ttlDays'], response);
                expect(contentType).toEqual('application/json');
                expect(latestLInk).toEqual(channelResource + '/latest');
                expect(name).toEqual(channelName);
                expect(ttlDays).toEqual(120);
            })
            .finally(done);
    });

});
