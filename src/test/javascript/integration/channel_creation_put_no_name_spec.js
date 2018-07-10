require('../integration_config');
const { getProp, getSelfLink } = require('../lib/helpers');

const getParsedPropFunc = parsed => prop => getProp(prop, parsed);

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

    utils.putChannel(channelName, function (response, body) {
        var parse = utils.parseJson(response, testName);
        returnedBody = parse;
        expect(getSelfLink(parse)).toEqual(channelResource);
        // getParsedProp(prop) safely returns parse.prop || null
        const getParsedProp = getParsedPropFunc(parse);
        expect(getParsedProp('ttlDays')).toEqual(120);
        expect(getParsedProp('description')).toEqual('');
        expect((getParsedProp('tags') || '').length).toEqual(0);
        expect(getParsedProp('replicationSource')).toEqual('');
    }, {});

    var newConfig = {
        description: 'yay put!',
        ttlDays: 5,
        tags: ['one', 'two']
    };

    utils.putChannel(channelName, function (response, body) {
        var parse = utils.parseJson(response, testName);
        expect(getSelfLink(parse)).toEqual(channelResource);
        const getParsedProp = getParsedPropFunc(parse);
        expect(getParsedProp('ttlDays')).toEqual(newConfig.ttlDays);
        expect(getParsedProp('description')).toEqual(newConfig.description);
        expect(getParsedProp('tags')).toEqual(newConfig.tags);
        expect(getParsedProp('creationDate')).toEqual(returnedBody.creationDate);
    }, newConfig);


});
