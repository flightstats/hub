require('../integration_config');
const { getProp, getSelfLink } = require('../lib/helpers');

var request = require('request');
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
    var returnedBody;

    var firstConfig = {
        ttlDays: 120,
    };

    utils.putChannel(channelName, function (response, body) {
        var parse = utils.parseJson(response, testName);
        returnedBody = parse;
        const getParsedProp = prop => getProp(prop, parse);
        expect(getSelfLink(parse)).toEqual(channelResource);
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
        expect(getSelfLink(parse)).toEqual(channelResource);
        expect(getParsedProp('ttlDays')).toEqual(0);
        expect(getParsedProp('maxItems')).toEqual(100);
    }, newConfig);


});
