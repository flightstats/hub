require('../integration_config');
const { getProp } = require('../lib/helpers');

var channel = utils.randomChannelName();
var moment = require('moment');

var tag = Math.random().toString().replace(".", "");
var testName = __filename;

/**
 * This should:
 * Create a channel with mutableTime
 * verify that mutableTime is returned from Put
 * verify that mutableTime is returned from Get
 *
 */
describe(testName, function () {

    var mutableTime = moment.utc().subtract(1, 'hours').format('YYYY-MM-DDTHH:mm:ss');
    const expected = mutableTime + '.000Z';

    var channelBody = {
        mutableTime: mutableTime,
        tags: [tag, "test"]
    };

    utils.putChannel(channel, function (response, body) {
        var parse = utils.parseJson(response, testName);
        expect(getProp('ttlDays', parse)).toBe(0);
        expect(getProp('maxItems', parse)).toBe(0);
        expect(getProp('mutableTime', parse)).toBe(expected);
    }, channelBody, testName);

    utils.getChannel(channel, function (response) {
        var parse = utils.parseJson(response, testName);
        expect(getProp('ttlDays', parse)).toBe(0);
        expect(getProp('maxItems', parse)).toBe(0);
        expect(getProp('mutableTime', parse)).toBe(mutableTime + '.000Z');

    }, testName)
});
