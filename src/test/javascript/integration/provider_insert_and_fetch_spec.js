require('../integration_config');
const {
    fromObjectPath,
    getProp,
} = require('../lib/helpers');
var channelName = utils.randomChannelName();
var providerResource = hubUrlBase + "/provider";
var channelResource = channelUrl + "/" + channelName;
var messageText = "MY SUPER TEST CASE: this & <that>. " + Math.random().toString();

describe(__filename, function () {

    it('inserts a value into a provider channel', function (done) {
        var url = providerResource;
        var headers = {
            'channelName': channelName,
            'Content-Type': 'text/plain'
        };
        var body = messageText;

        utils.httpPost(url, headers, body)
            .then(function (response) {
                expect(getProp('statusCode', response)).toEqual(200);
            })
            .finally(done);
    });

    it('verifies the value was inserted', function (done) {
        var url = channelResource + '/latest?stable=false';

        utils.httpGet(url)
            .then(utils.followRedirectIfPresent)
            .then(function (response) {
                const contentType = fromObjectPath(['headers', 'content-type'], response);
                expect(getProp('statusCode', response)).toEqual(200);
                expect(contentType).toEqual('text/plain');
                expect(getProp('body', response)).toContain(messageText);
            })
            .finally(done);
    });

});
