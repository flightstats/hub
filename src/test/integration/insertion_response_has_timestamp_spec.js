require('./integration_config.js');

var channelName = utils.randomChannelName();
var thisChannelResource = channelUrl + "/" + channelName;
var messageText = "MY SUPER TEST CASE: this & <that>. " + Math.random().toString();
var testName = __filename;

utils.configureFrisby();

utils.runInTestChannel(testName, channelName, function () {
    frisby.create(testName + ': Inserting a value into a channel.')
        .post(thisChannelResource, null, { body : messageText})
        .addHeader("Content-Type", "text/plain")
        .expectStatus(201)
        .expectHeader('content-type', 'application/json')
        .expectJSON('_links', {
            channel : {
                href : thisChannelResource
            }
        })
        .expectJSON({
            timestamp : function (value) {
                expect(value).toMatch(/^\d\d\d\d-\d\d-\d\dT\d\d:\d\d:\d\d\.\d\d\dZ$/);
            }
        })
        .toss();
});
