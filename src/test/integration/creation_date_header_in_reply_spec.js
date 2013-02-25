var utils = require('./utils.js');
require('./integration_config.js');
var frisby = require('frisby');

var channelName = utils.randomChannelName();
var thisChannelResource = channelUrl + "/" + channelName;
var messageText = "there's a snake in my boot!";

utils.runInTestChannel(channelName, function () {

    console.info('Inserting a value...');
    frisby.create('Inserting a value into a channel.')
        .post(thisChannelResource, null, { body: messageText})
        .addHeader("Content-Type", "text/plain")
        .expectStatus(200)
        .afterJSON(function (result) {
            var valueUrl = result['_links']['self']['href'];
            frisby.create('Fetching value in order to check creation date.')
                .get(valueUrl)
                .expectStatus(200)
                // Wishing frisby allowed callbacks for header validation too...but it doesn't yet.
                .expectHeaderContains('creation-date', 'T')
                .toss();
        })
        .inspectJSON()
        .toss();
});