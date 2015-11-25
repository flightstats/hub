require('./integration_config.js');

var request = require('request');
var channelName = utils.randomChannelName();
var channelResource = channelUrl + "/" + channelName;
var testName = __filename;
utils.configureFrisby();


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
        expect(parse._links.self.href).toEqual(channelResource);
        expect(parse.ttlDays).toEqual(120);
        expect(parse.maxItems).toEqual(0);
        expect(parse.replicationSource).toEqual('');
    }, firstConfig);

    var newConfig = {
        ttlDays: 0,
        maxItems: 100,
    };

    utils.putChannel(channelName, function (response, body) {
        var parse = utils.parseJson(response, testName);
        expect(parse._links.self.href).toEqual(channelResource);
        expect(parse.ttlDays).toEqual(0);
        expect(parse.maxItems).toEqual(100);
    }, newConfig);


});
