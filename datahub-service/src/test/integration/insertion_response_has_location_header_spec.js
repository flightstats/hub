require('./integration_config.js');

var channelName = utils.randomChannelName();
var thisChannelResource = channelUrl + "/" + channelName;
var messageText = "MY SUPER TEST CASE: this & <that>. " + Math.random().toString();
var testName = "insertion_response_has_location_header_spec";

utils.configureFrisby();

utils.runInTestChannel(channelName, function () {
    frisby.create(testName + ': Inserting a value into a channel.')
        .post(thisChannelResource, null, { body: messageText})
        .addHeader("Content-Type", "text/plain")
        .expectStatus(201)
        .after(function (err, res, body) {
            var regex = new RegExp("^" + thisChannelResource.replace(/\//g, "\\/").replace(/\:/g, "\\:") + "\\/[0-9]$");
            expect(res.headers['location']).toMatch(regex);
        })
        .toss();
});
