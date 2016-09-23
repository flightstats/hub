require('./integration_config.js');

var request = require('request');
var channelName = utils.randomChannelName();
var channelResource = channelUrl + "/" + channelName;
var testName = __filename;

/**
 * create a channel via put with allowDataLoss == false
 * verify that we can not change some settings (storage to BATCH)
 * should not be able to change allowDataLoss to true
 *
 */
describe(testName, function () {
    var returnedBody;

    utils.putChannel(channelName, function (response, body) {
        var parse = utils.parseJson(response, testName);
        returnedBody = parse;

        expect(parse._links.self.href).toEqual(channelResource);
        expect(parse.ttlDays).toEqual(120);
        expect(parse.description).toEqual('');
        expect(parse.tags.length).toEqual(2);
        expect(parse.storage).toEqual('SINGLE');
        expect(parse.allowDataLoss).toEqual(true);
    }, {tags: ['one', 'two']});

    utils.putChannel(channelName, function (response, body) {
        var parse = utils.parseJson(response, testName);
        expect(parse._links.self.href).toEqual(channelResource);
        expect(parse.allowDataLoss).toEqual(false);

    }, {allowDataLoss: false});

    utils.putChannel(channelName, false, {allowDataLoss: true}, 'allowDataLoss', 400);

    utils.putChannel(channelName, false, {storage: 'BATCH'}, 'storage Batch', 400);
    utils.putChannel(channelName, false, {tags: ['one']}, 'tag removal', 400);
    utils.putChannel(channelName, false, {ttlDays: 119}, 'ttlDays', 400);

    utils.putChannel(channelName, function (response, body) {
        var parse = utils.parseJson(response, testName);
        expect(parse._links.self.href).toEqual(channelResource);
        expect(parse.tags.length).toEqual(3);
        expect(parse.storage).toEqual('BOTH');

    }, {storage: 'BOTH', tags: ['one', 'two', 'three']}, 'storage and tags', 201);

});


