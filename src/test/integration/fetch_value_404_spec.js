require('./integration_config.js');

var channelName = utils.randomChannelName();
var badValueUrl = channelUrl + "/" + channelName + "/2014/12/31/23/59/59/999/foooo" + Math.random().toString();
var testName = __filename;

utils.configureFrisby();

utils.runInTestChannel(testName, channelName, function () {
    frisby.create(testName + ': Fetching a nonexistent value.')
        .get(badValueUrl)
        .expectStatus(404)
        .toss();
});
