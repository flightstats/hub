var utils = require('./utils.js');
var frisby = require('frisby');

var channelName = "no_way_jose90928280xFF";
var thisChannelResource = channelUrl + "/" + channelName;

utils.configureFrisby();

frisby.create('Fetching channel metadata for nonexistent channel')
    .post(thisChannelResource)
    .expectStatus(404)
    .toss();
