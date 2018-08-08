require('../integration_config');
const {
    followRedirectIfPresent,
    fromObjectPath,
    getProp,
    hubClientGet,
} = require('../lib/helpers');

const channelName = utils.randomChannelName();
const channelResource = channelUrl + "/" + channelName;
const messageText = "MY SUPER TEST CASE: this & <that>. " + Math.random().toString();
const defaultHeaders = { 'Content-Type': 'application/json' };
describe(__filename, function () {
    it('creates a channel', function (done) {
        var url = channelUrl;
        var body = {'name': channelName};

        utils.httpPost(url, defaultHeaders, body)
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

    it('verifies the channel metadata is accurate', async () => {
        const url = `${channelResource}/`;
        const res = await hubClientGet(url, defaultHeaders);
        const response = await followRedirectIfPresent(res, defaultHeaders);
        const contentType = fromObjectPath(['headers', 'content-type'], response);
        const latestLInk = fromObjectPath(['body', '_links', 'latest', 'href'], response);
        const name = fromObjectPath(['body', 'name'], response);
        const ttlDays = fromObjectPath(['body', 'ttlDays'], response);
        expect(getProp('statusCode', response)).toEqual(200);
        expect(contentType).toEqual('application/json');
        expect(latestLInk).toEqual(`${channelResource}/latest`);
        expect(name).toEqual(channelName);
        expect(ttlDays).toEqual(120);
    });
});
