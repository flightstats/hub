require('./integration_config.js');

var channelName = utils.randomChannelName();
var badValueUrl = channelUrl + "/" + channelName + "/foooo" + Math.random().toString();
var testName = "fetch_value_404_spec";

utils.configureFrisby();

utils.runInTestChannel(channelName, function () {
    frisby.create(testName + ': Fetching a nonexistent value.')
        .get(badValueUrl)
        .expectStatus(404)
        .toss();
});
