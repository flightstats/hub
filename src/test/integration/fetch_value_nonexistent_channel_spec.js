require('./integration_config.js');
var frisby = require('frisby');
var utils = require("./utils.js");

var channelName = utils.randomChannelName();
var badValueUrl = channelUrl + "/" + channelName + "/685221b0-77c2-11e2-8a3e-20c9d08600a5";

utils.runInTestChannel(channelName, function () {
    frisby.create('Fetching a nonexistent channel.')
        .get(badValueUrl)
        .expectStatus(404)
        .toss();
});
