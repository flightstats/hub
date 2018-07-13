require('../integration_config');
const {
    fromObjectPath,
    getProp,
} = require('../lib/helpers');

var channelName = utils.randomChannelName();
var channelResource = channelUrl + '/' + channelName;

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

    it('fetches the list of channels', function (done) {
        utils.httpGet(channelUrl)
            .then(function (response) {
                expect(getProp('statusCode', response)).toEqual(200);
                const contentType = fromObjectPath(['headers', 'content-type'], response);
                const links = fromObjectPath(['body', '_links'], response) || {};
                const { channels = [], self = {} } = links;
                expect(contentType).toEqual('application/json');
                expect(self.href).toEqual(channelUrl);
                var channelURLs = channels.map(function (obj) {
                    return getProp('href', obj) || '';
                });
                expect(channelURLs).toContain(channelResource);
            })
            .finally(done);
    });

});
