require('./integration_config.js');
var frisby = require('frisby');
var utils = require('./utils.js');

var channelName = utils.randomChannelName();

utils.configureFrisby();

utils.runInTestChannel(channelName, function (channelResponse) {
    var channelResource = channelResponse['_links']['self']['href'];
    var latestResource = channelResponse['_links']['latest']['href'];
    var messageText = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAA_bee_buzzzzzzzz";
    frisby.create('Inserting latest item')
        .post(channelResource, null, { body: messageText})
        .addHeader("Content-Type", "text/plain")
        .expectStatus(200)
        .afterJSON(function (response) {
            var itemUrl = response['_links']['self']['href'];
            frisby.create('Fetching latest item from channel')
                .get(latestResource, {followRedirect: false})
                .expectStatus(303)
                .expectHeader('location', itemUrl)
                .toss();
        })
        .toss();
});

