require('./integration_config.js');

var channelName = utils.randomChannelName();
var badValueUrl = channelUrl + "/" + channelName + "/685221b0-77c2-11e2-8a3e-20c9d08600a5";
var testName = "fetch_value_nonexistant_channel_spec";

utils.configureFrisby();

utils.runInTestChannel(channelName, function () {
    frisby.create(testName + ':Fetching a nonexistent channel.')
        .get(badValueUrl)
        .expectStatus(404)
        .toss();
});
