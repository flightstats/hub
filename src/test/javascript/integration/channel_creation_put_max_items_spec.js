require('../integration_config');
const { fromObjectPath, getProp } = require('../lib/helpers');

var channelName = utils.randomChannelName();
var channelResource = channelUrl + "/" + channelName;
var testName = __filename;
/**
 * create a channel via put
 * verify that it exists
 * change the channel via put
 * verify that the new config exists
 */
describe(testName, function () {
    var firstConfig = {
        ttlDays: 120,
    };

    utils.putChannel(channelName, function (response, body) {
        var parse = utils.parseJson(response, testName);
        const getParsedProp = prop => getProp(prop, parse);
        expect(fromObjectPath(['_links', 'self', 'href'], parse)).toEqual(channelResource);
        expect(getParsedProp('ttlDays')).toEqual(120);
        expect(getParsedProp('maxItems')).toEqual(0);
        expect(getParsedProp('replicationSource')).toEqual('');
    }, firstConfig);

    var newConfig = {
        ttlDays: 0,
        maxItems: 100,
    };

    utils.putChannel(channelName, function (response, body) {
        var parse = utils.parseJson(response, testName);
        const getParsedProp = prop => getProp(prop, parse);
        expect(fromObjectPath(['_links', 'self', 'href'], parse)).toEqual(channelResource);
        expect(getParsedProp('ttlDays')).toEqual(0);
        expect(getParsedProp('maxItems')).toEqual(100);
    }, newConfig);
});
