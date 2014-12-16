require('./integration_config.js');

var request = require('request');
var channelName = utils.randomChannelName();
var jsonBody = JSON.stringify({ "name": channelName});
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

    utils.putChannel(channelName, function (response, body) {
        var parse = JSON.parse(body);
        returnedBody = parse;
        expect(parse._links.self.href).toEqual(channelResource);
        expect(parse.ttlDays).toEqual(120);
        expect(parse.description).toEqual('');
        expect(parse.tags.length).toEqual(0);
    });

    var newConfig = {
        description: 'yay put!',
        ttlDays: 5,
        tags: ['one', 'two']
    };

    utils.putChannel(channelName, function (response, body) {
        var parse = JSON.parse(body);
        expect(parse._links.self.href).toEqual(channelResource);
        expect(parse.ttlDays).toEqual(newConfig.ttlDays);
        expect(parse.description).toEqual(newConfig.description);
        expect(parse.tags).toEqual(newConfig.tags);
        expect(parse.creationDate).toEqual(returnedBody.creationDate);
    }, newConfig);


});
