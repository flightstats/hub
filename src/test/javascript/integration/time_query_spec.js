require('../integration_config');
const {
    fromObjectPath,
    getProp,
} = require('../lib/helpers');
var request = require('request');
var channelName = utils.randomChannelName();
var channelResource = channelUrl + "/" + channelName;
var testName = __filename;

/**
 * This should:
 *
 * 1 - create a channel
 * 2 - post items into the channel
 * 3 - verify that records are returned via time query
 */
describe(testName, function () {
    utils.createChannel(channelName);

    it('queries before insertion', function (done) {
        request.get({url : channelResource + '/time/minute?stable=false', json : true},
            function (err, response, body) {
                expect(err).toBeNull();
                expect(getProp('statusCode', response)).toBe(200);
                const uris = fromObjectPath(['_links', 'uris'], body);
                const urisLength = !!uris && uris.length === 0;
                expect(urisLength).toBe(true);
                done();
            });
    });

    for (var i = 0; i < 4; i++) {
        utils.addItem(channelResource);
    }

    function callTime(url, items, calls, done) {
        request.get({url: url, json: true},
            function (err, response, body) {
                expect(err).toBeNull();
                expect(getProp('statusCode', response)).toBe(200);
                calls++;
                const uris = fromObjectPath(['_links', 'uris'], body);
                const prevLink = fromObjectPath(['_links', 'previous', 'href'], body);
                items = items.concat(uris);
                if (items.length === 4) {
                    done();
                } else if (calls < 10) {
                    callTime(prevLink, items, calls, done);
                } else {
                    done('unable to find 4 items in ' + calls + ' calls');
                }
            });
    }

    it('gets items from channel second', function (done) {
        callTime(channelResource + '/time/second?stable=false', [], 0, done);
    });

    it('gets items from channel minute', function (done) {
        callTime(channelResource + '/time/minute?stable=false', [], 0, done);
    });

    it('gets items from channel hour', function (done) {
        callTime(channelResource + '/time/hour?stable=false', [], 0, done);
    });

    it('gets items from channel day', function (done) {
        callTime(channelResource + '/time/day?stable=false', [], 0, done);
    });

});
