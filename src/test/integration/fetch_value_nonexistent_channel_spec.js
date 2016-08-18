require('./integration_config.js');

var channelName = utils.randomChannelName();
var badValueUrl = channelUrl + "/" + channelName + "/2014/12/31/23/59/59/999/685221b0-77c2-11e2-8a3e-20c9d08600a5";
var testName = __filename;

utils.configureFrisby();

utils.runInTestChannel(testName, channelName, function () {
    frisby.create(testName + ':Fetching a nonexistent channel.')
        .get(badValueUrl)
        .expectStatus(404)
        .toss();
});
