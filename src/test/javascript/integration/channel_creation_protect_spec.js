require('../integration_config');
const {
  getProp,
  fromObjectPath,
} = require('../lib/helpers');

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
        const selfLink = fromObjectPath(['_links', 'self', 'href'], parse);
        expect(selfLink).toEqual(channelResource);
        expect(getProp('ttlDays', parse)).toEqual(120);
        expect(getProp('description', parse)).toEqual('');
        expect((getProp('tags', parse) || '').length).toEqual(2);
        expect(getProp('storage', parse)).toEqual('SINGLE');
        expect(getProp('protect', parse)).toEqual(false);
    }, {tags: ['one', 'two']});

    utils.putChannel(channelName, function (response, body) {
        var parse = utils.parseJson(response, testName);
        const selfLink = fromObjectPath(['_links', 'self', 'href'], parse);
        expect(selfLink).toEqual(channelResource);
        expect(getProp('protect', parse)).toEqual(true);

    }, {protect: true, owner: 'someone'});

    utils.putChannel(channelName, false, {protect: false}, 'protect', 403);

    utils.putChannel(channelName, false, {storage: 'BATCH'}, 'storage Batch', 403);
    utils.putChannel(channelName, false, {tags: ['one']}, 'tag removal', 403);
    utils.putChannel(channelName, false, {ttlDays: 119}, 'ttlDays', 403);
    utils.putChannel(channelName, false, {owner: 'CBA'}, 'owner', 403);

    utils.putChannel(channelName, function (response, body) {
        var parse = utils.parseJson(response, testName);
        const selfLink = fromObjectPath(['_links', 'self', 'href'], parse);
        expect(selfLink).toEqual(channelResource);
        expect((getProp('tags', parse) || '').length).toEqual(3);
        expect(getProp('storage', parse)).toEqual('BOTH');

    }, {storage: 'BOTH', tags: ['one', 'two', 'three']}, 'storage and tags', 201);

    it("deletes channel " + channelName, function (done) {
        request.del({url: channelResource},
            function (err, response, body) {
                console.log('body', body);
                expect(getProp('statusCode', response)).toBe(403);
                done();
            });
    });
});
