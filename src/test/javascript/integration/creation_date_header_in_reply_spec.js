require('../integration_config');
const {
    fromObjectPath,
    getProp,
    hubClientGet,
} = require('../lib/helpers');

var channelName = utils.randomChannelName();
const channelResource = `${channelUrl}/${channelName}`;
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

    it('verifies the creation-date header is returned', async () => {
        if (!itemURL) return fail('itemURL failed initialization in previous test');
        const response = await hubClientGet(itemURL);
        expect(getProp('statusCode', response)).toEqual(200);
        console.log('response', response.headers);
        const creationDate = fromObjectPath(['headers', 'creation-date'], response);
        expect(creationDate).toContain('T');
    });
});
