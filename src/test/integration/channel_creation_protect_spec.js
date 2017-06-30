require('./integration_config.js');

var request = require('request');
var channelName = utils.randomChannelName();
var channelResource = channelUrl + "/" + channelName;
var testName = __filename;

/**
 * create a channel via put with protect == true
 * verify that we can not change some settings (storage to BATCH)
 * should not be able to change protect to false
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
        expect(parse.protect).toEqual(false);
    }, {tags: ['one', 'two']});

    utils.putChannel(channelName, function (response, body) {
        var parse = utils.parseJson(response, testName);
        expect(parse._links.self.href).toEqual(channelResource);
        expect(parse.protect).toEqual(true);

    }, {protect: true, owner: 'someone'});

    utils.putChannel(channelName, false, {protect: false}, 'protect', 403);

    utils.putChannel(channelName, false, {storage: 'BATCH'}, 'storage Batch', 403);
    utils.putChannel(channelName, false, {tags: ['one']}, 'tag removal', 403);
    utils.putChannel(channelName, false, {ttlDays: 119}, 'ttlDays', 403);
    utils.putChannel(channelName, false, {owner: 'CBA'}, 'owner', 403);

    utils.putChannel(channelName, function (response, body) {
        var parse = utils.parseJson(response, testName);
        expect(parse._links.self.href).toEqual(channelResource);
        expect(parse.tags.length).toEqual(3);
        expect(parse.storage).toEqual('BOTH');

    }, {storage: 'BOTH', tags: ['one', 'two', 'three']}, 'storage and tags', 201);

    it("deletes channel " + channelName, function (done) {
        request.del({url: channelResource},
            function (err, response, body) {
                console.log('body', body);
                expect(response.statusCode).toBe(403);
                done();
            });
    });
});


