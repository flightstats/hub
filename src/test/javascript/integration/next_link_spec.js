require('../integration_config');
const {
    createChannel,
    fromObjectPath,
    getProp,
} = require('../lib/helpers');

var channelName = utils.randomChannelName();
var channelResource = channelUrl + "/" + channelName;
var testName = __filename;
let createdChannel = false;

describe(testName, function () {
    beforeAll(async () => {
        const channel = await createChannel(channelName);
        if (getProp('status', channel)) {
            createdChannel = true;
            console.log(`created channel for ${__filename}`);
        }
    });

    var items = [];
    var headers = {'Content-Type': 'application/json'};
    var body = {'name': channelName};

    function postOneItem (done) {
        utils.httpPost(channelResource, headers, body)
            .then(function (response) {
                expect(getProp('statusCode', response)).toEqual(201);
                const selfLink = fromObjectPath(['body', '_links', 'self', 'href'], response);
                items.push(selfLink);
            })
            .finally(done);
    }
    it('posts item', function (done) {
        postOneItem(done);
    });

    it('gets 404 from /next ', function (done) {
        if (!createdChannel) return done.fail('channel not created in before block');
        utils.httpGet(items[0] + '/next', headers, body)
            .then(function (response) {
                expect(getProp('statusCode', response)).toEqual(404);
            })
            .finally(done);
    });

    it('gets empty list from /next/2 ', function (done) {
        if (!createdChannel) return done.fail('channel not created in before block');
        utils.httpGet(items[0] + '/next/2', headers, body)
            .then(function (response) {
                expect(getProp('statusCode', response)).toEqual(200);
                const uris = fromObjectPath(['body', '_links', 'uris'], response);
                const urisLength = !!uris && uris.length === 0;
                expect(urisLength).toBe(true);
            })
            .finally(done);
    });

    it('posts item', function (done) {
        if (!createdChannel) return done.fail('channel not created in before block');
        postOneItem(done);
    });

    it('posts item', function (done) {
        if (!createdChannel) return done.fail('channel not created in before block');
        postOneItem(done);
    });

    it('gets item from /next ', function (done) {
        if (!createdChannel) return done.fail('channel not created in before block');
        utils.httpGet(items[0] + '/next?stable=false', headers, body)
            .then(function (response) {
                expect(getProp('statusCode', response)).toEqual(303);
                const location = fromObjectPath(['headers', 'location'], response);
                expect(location).toBe(items[1]);
            })
            .finally(done);
    });

    it('gets items from /next/2 ', function (done) {
        if (!createdChannel) return done.fail('channel not created in before block');
        utils.httpGet(items[0] + '/next/2?stable=false', headers, body)
            .then(function (response) {
                expect(getProp('statusCode', response)).toEqual(200);
                const uris = fromObjectPath(['body', '_links', 'uris'], response) || [];
                expect(uris.length).toBe(2);
                expect(uris[0]).toBe(items[1]);
                expect(uris[1]).toBe(items[2]);
            })
            .finally(done);
    });

    it('gets inclusive items from /next/2 ', function (done) {
        if (!createdChannel) return done.fail('channel not created in before block');
        utils.httpGet(items[0] + '/next/2?stable=false&inclusive=true', headers, body)
            .then(function (response) {
                expect(getProp('statusCode', response)).toEqual(200);
                const uris = fromObjectPath(['body', '_links', 'uris'], response) || [];
                expect(uris.length).toBe(2);
                expect(uris[0]).toBe(items[0]);
                expect(uris[1]).toBe(items[1]);
            })
            .finally(done);
    });
});
