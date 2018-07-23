require('../integration_config');
const {
    fromObjectPath,
    getProp,
} = require('../lib/helpers');

var request = require('request');
var moment = require('moment');
var channelName = utils.randomChannelName();
var channelResource = channelUrl + "/" + channelName;
var testName = __filename;

describe(testName, function () {
    utils.putChannel(channelName, function () {
    }, {"name": channelName, ttlDays: 1});

    function time(text) {
        console.log(moment().format('h:mm:ss.SSS'), text);
    }

    it('adds items and traverses previous links', function (done) {
        var values = [];
        var items = [];
        time('starting');
        utils.postItemQ(channelResource)
            .then(function (value) {
                values.push(value);
                const href = fromObjectPath(['body', '_links', 'self', 'href'], value);
                items.push(href);
                time('getting self');
                return getItem(href, 200, '0');
            })
            .then(function (value) {
                time('getting previousA');
                return getItem(items[0] + '/previous', 404, 'A');
            })
            .then(function (value) {
                time('getting previousA2');
                return getItem(items[0] + '/previous/2', 200, 'B');
            })
            .then(function (value) {
                time('posting1');
                const uris = fromObjectPath(['body', '_links', 'uris'], value);
                const urisLength = !!uris && uris.length === 0;
                expect(urisLength).toBe(true);
                return utils.postItemQ(channelResource);
            })
            .then(function (value) {
                time('posting2');
                const href = fromObjectPath(['body', '_links', 'self', 'href'], value);
                items.push(href);
                return utils.postItemQ(channelResource);
            })
            .then(function (value) {
                const href = fromObjectPath(['body', '_links', 'self', 'href'], value);
                items.push(href);
                time('getting previousB');
                return getItem(items[2] + '/previous', 200, 'C');
            })
            .then(function (value) {
                time('getting previousB2');
                const href = fromObjectPath(['response', 'request', 'href'], value);
                expect(href).toBe(items[1]);
                return getItem(items[2] + '/previous/2', 200);
            })
            .then(function (value) {
                time('verifying');
                const href = fromObjectPath(['body', '_links', 'previous', 'href'], value);
                const uris = fromObjectPath(['body', '_links', 'uris'], value) || [];
                expect(uris.length).toBe(2);
                expect(uris[0]).toBe(items[0]);
                expect(uris[1]).toBe(items[1]);
                expect(href).toBe(items[0] + '/previous/2?stable=false');
                done();
            });
    }, 2 * 60001);

    function getItem(url, status) {
        status = status || 200;
        return new Promise((resolve, reject) => {
            request.get({
                url: url + '?stable=false',
                json: true
            }, (err, response, body) => {
                expect(err).toBeNull();
                expect(getProp('statusCode', response)).toBe(status);
                resolve({response: response, body: body});
            });
        });
    }

});
