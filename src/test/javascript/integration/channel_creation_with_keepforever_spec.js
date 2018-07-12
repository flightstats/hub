require('../integration_config');
const {
    fromObjectPath,
    getProp,
} = require('../lib/helpers');

var channelName = utils.randomChannelName();
var channelResource = channelUrl + "/" + channelName;

describe(__filename, function () {
    it('creates a channel with keepForever', function (done) {
        var url = channelUrl;
        var headers = {'Content-Type': 'application/json'};
        var body = {'name': channelName, 'keepForever': true};

        utils.httpPost(url, headers, body)
            .then(function (response) {
                const contentType = fromObjectPath(['headers', 'content-type'], response);
                const ttlDays = fromObjectPath(['body', 'ttlDays'], response);
                const keepForever = fromObjectPath(['body', 'keepForever'], response);
                expect(getProp('statusCode', response)).toEqual(201);
                expect(contentType).toEqual('application/json');
                expect(ttlDays).toEqual(0);
                expect(keepForever).toEqual(true);
            })
            .finally(done);
    });

    it('verifies the channel does exist', function (done) {
        utils.httpGet(channelResource)
            .then(function (response) {
                const contentType = fromObjectPath(['headers', 'content-type'], response);
                const ttlDays = fromObjectPath(['body', 'ttlDays'], response);
                const keepForever = fromObjectPath(['body', 'keepForever'], response);
                const name = fromObjectPath(['body', 'name'], response);
                expect(response.statusCode).toEqual(200);
                expect(contentType).toEqual('application/json');
                expect(name).toEqual(channelName);
                expect(ttlDays).toEqual(0);
                expect(keepForever).toEqual(true);
            })
            .finally(done);
    });
});
