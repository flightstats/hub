require('../integration_config');
const {
    fromObjectPath,
    getProp,
} = require('../lib/helpers');

var channelName = utils.randomChannelName();
const channelResource = `${channelUrl}/${channelName}`;
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

    it('inserts an item', function (done) {
        var url = channelResource;
        var headers = {'Content-Type': 'text/plain'};
        var body = messageText;

        utils.httpPost(url, headers, body)
            .then(function (response) {
                expect(getProp('statusCode', response)).toEqual(201);
                const location = fromObjectPath(['headers', 'location'], response);
                expect(location).toBeDefined();
            })
            .finally(done);
    });

});
