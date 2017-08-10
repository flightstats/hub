require('../integration_config');

var request = require('request');
var http = require('http');
var parse = require('parse-link-header');
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

    utils.putChannel(channel, false, channelBody, testName);

    utils.itRefreshesChannels();

    it('get all tags ' + tag, function (done) {
        getAndMatch(hubUrlBase + '/tag', 'tags', tag, done);
    }, 60001);

    it('gets tag ' + tag, function (done) {
        getAndMatch(hubUrlBase + '/tag/' + tag, 'channels', channel, done);
    }, 60001);

    function getAndMatch(url, nodeName, name, done) {
        name = name || tag;
        console.log('calling', url);
        request.get(url,
            function (err, response, body) {
                expect(err).toBeNull();
                expect(response.statusCode).toBe(200);
                if (response.statusCode == 200) {
                    body = utils.parseJson(response, testName);
                    var tags = body._links[nodeName];
                    var found = tags.find(tag => tag.name === name);
                    console.log("found ", found);
                    expect(found.name).toBe(name);
                } else {
                    expect(name).toBe(false);
                }
                done();
            });
    }

});

