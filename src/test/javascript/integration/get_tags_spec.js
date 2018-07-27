require('../integration_config');
const {
    fromObjectPath,
    getProp,
} = require('../lib/helpers');

var request = require('request');
// var http = require('http'); // TODO: unused?
// var parse = require('parse-link-header'); // TODO: unused?
var channel = utils.randomChannelName();
var tag = Math.random().toString().replace(".", "");
var testName = __filename;
var channelBody = {
    tags: [tag, "test"],
    ttlDays: 1
};

/**
 * This should:
 * Create Channel with tag Tag
 *
 * Get all the tags and expect Channel to be in the list
 *
 * Get tag and make sure channel is in the list.
 *
 */
describe(testName, function () {
    function getAndMatch (url, nodeName, name, done) {
        name = name || tag;
        console.log('calling', url);
        request.get(url,
            function (err, response, body) {
                expect(err).toBeNull();
                const statusCode = getProp('statusCode', response);
                expect(statusCode).toBe(200);
                if (statusCode === 200) {
                    body = utils.parseJson(response, testName);
                    const tags = fromObjectPath(['_links', nodeName], body) || [];
                    var found = tags.find(tag => getProp('name', tag) === name);
                    console.log("found ", found);
                    expect(getProp('name', found)).toBe(name);
                } else {
                    expect(name).toBe(false);
                }
                done();
            });
    }

    utils.putChannel(channel, false, channelBody, testName);

    utils.itRefreshesChannels();

    it('get all tags ' + tag, function (done) {
        getAndMatch(hubUrlBase + '/tag', 'tags', tag, done);
    }, 60001);

    it('gets tag ' + tag, function (done) {
        getAndMatch(hubUrlBase + '/tag/' + tag, 'channels', channel, done);
    }, 60001);

});
