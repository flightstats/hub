require('../integration_config');
const {
    fromObjectPath,
    getProp,
} = require('../lib/helpers');

var channelName = utils.randomChannelName();
var channelResource = channelUrl + "/" + channelName;
var testName = __filename;

describe(testName, function () {
    utils.putChannel(channelName, function () {
    }, {"name": channelName, ttlDays: 1});

    var items = [];
    var headers = {'Content-Type': 'application/json'};
    var body = {'name': channelName};

    function postOneItem(done) {
        utils.httpPost(channelResource, headers, body)
            .then(function (response) {
                expect(getProp('statusCode', response)).toEqual(201);
                const href = fromObjectPath(['body', '_links', 'self', 'href'], response);
                items.push(href);
            })
            .finally(done);
    }

    it('posts item', function (done) {
        postOneItem(done);
    });

    it('gets 404 from /previous ', function (done) {
        utils.httpGet(items[0] + '/previous', headers, body)
            .then(function (response) {
                expect(getProp('statusCode', response)).toEqual(404);
            })
            .finally(done);
    });

    it('gets empty list from /previous/2 ', function (done) {
        utils.httpGet(items[0] + '/previous/2', headers, body)
            .then(function (response) {
                expect(getProp('statusCode', response)).toEqual(200);
                const uris = fromObjectPath(['body', '_links', 'uris'], response);
                const urisLength = !!uris && uris.length === 0;
                expect(urisLength).toBe(true);
            })
            .finally(done);
    });

    it('posts item', function (done) {
        postOneItem(done);
    });

    it('posts item', function (done) {
        postOneItem(done);
    });

    it('gets item from /previous ', function (done) {
        utils.httpGet(items[2] + '/previous?stable=false', headers, body)
            .then(function (response) {
                expect(getProp('statusCode', response)).toEqual(303);
                const location = fromObjectPath(['headers', 'location'], response);
                expect(location).toBe(items[1]);
            })
            .finally(done);
    });

    it('gets items from /previous/2 ', function (done) {
        utils.httpGet(items[2] + '/previous/2?stable=false', headers, body)
            .then(function (response) {
                expect(getProp('statusCode', response)).toEqual(200);
                const uris = fromObjectPath(['body', '_links', 'uris'], response) || [];
                expect(uris.length).toBe(2);
                expect(uris[0]).toBe(items[0]);
                expect(uris[1]).toBe(items[1]);
            })
            .finally(done);
    });

    it('gets inclusive items from /previous/2 ', function (done) {
        utils.httpGet(items[2] + '/previous/2?stable=false&inclusive=true', headers, body)
            .then(function (response) {
                expect(getProp('statusCode', response)).toEqual(200);
                const uris = fromObjectPath(['body', '_links', 'uris'], response) || [];
                expect(uris.length).toBe(2);
                expect(uris[0]).toBe(items[1]);
                expect(uris[1]).toBe(items[2]);
            })
            .finally(done);
    });

});
