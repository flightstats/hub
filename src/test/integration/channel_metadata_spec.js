var utils = require('./utils.js');
var frisby = require('frisby');

var channelName = utils.randomChannelName();
var thisChannelResource = channelUrl + "/" + channelName;
var messageText = "MY SUPER TEST CASE: this & <that>. " + Math.random().toString();

utils.configureFrisby();

utils.runInTestChannel(channelName, function () {
    frisby.create('Inserting a value into a channel.')
        .post(thisChannelResource, null, { body: messageText})
        .addHeader("Content-Type", "text/plain")
        .expectStatus(200)
        .afterJSON(function (result) {
            var valueUrl = result['_links']['self']['href'];
            frisby.create('Now fetching metadata')
                .get(thisChannelResource)
                .expectStatus(200)
                .expectHeader('content-type', 'application/json')
                .expectJSON('_links.latest', {
                    href: thisChannelResource + '/latest'
                })
                .expectJSON('_links.ws', {
                    href: thisChannelResource.replace(/^http/, "ws") + '/ws'
                })
                .expectJSON({"name": channelName})
                //TODO: Validate creation date and last update date
                .toss();
        })
        .toss();
});
