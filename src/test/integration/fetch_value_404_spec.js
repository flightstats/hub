require('./integration_config.js');

var channelName = utils.randomChannelName();
var badValueUrl = channelUrl + "/" + channelName + "/foooo" + Math.random().toString();
var testName = __filename;

utils.configureFrisby();

utils.runInTestChannel(testName, channelName, function () {
    frisby.create(testName + ': Fetching a nonexistent value.')
        .get(badValueUrl)
        .expectStatus(404)
        .toss();
});
