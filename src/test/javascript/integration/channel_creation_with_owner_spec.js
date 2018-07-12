require('../integration_config');
const {
    fromObjectPath,
    getProp,
} = require('../lib/helpers');

var channelName = utils.randomChannelName();
var channelResource = channelUrl + "/" + channelName;

describe(__filename, function () {
    it('verifies the channel doesn\'t exist yet', function (done) {
        utils.httpGet(channelResource)
            .then(function (response) {
                expect(getProp('statusCode', response)).toEqual(404);
            })
            .finally(done);
    });

    it('creates a channel with an owner', function (done) {
        var url = channelUrl;
        var headers = {'Content-Type': 'application/json'};
        var body = {'name': channelName, 'owner': 'pwned'};

        utils.httpPost(url, headers, body)
            .then(function (response) {
                const contentType = fromObjectPath(['headers', 'content-type'], response);
                const owner = fromObjectPath(['body', 'owner'], response);
                expect(getProp('statusCode', response)).toEqual(201);
                expect(contentType).toEqual('application/json');
                expect(owner).toEqual('pwned');
            })
            .finally(done);
    });

    it('verifies the channel does exist', function (done) {
        utils.httpGet(channelResource)
            .then(function (response) {
                const contentType = fromObjectPath(['headers', 'content-type'], response);
                const name = fromObjectPath(['body', 'name'], response);
                const owner = fromObjectPath(['body', 'owner'], response);
                expect(getProp('statusCode', response)).toEqual(200);
                expect(contentType).toEqual('application/json');
                expect(name).toEqual(channelName);
                expect(owner).toEqual('pwned');
            })
            .finally(done);
    });
});
