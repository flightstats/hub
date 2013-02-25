var utils = require('./utils.js');
var frisby = require('frisby');

var channelName = utils.randomChannelName();
var thisChannelResource = channelUrl + "/" + channelName;
var messageText = "MY SUPER TEST CASE: this & <that>. " + Math.random().toString();

utils.runInTestChannel(channelName, function () {

    console.info('Inserting a value...');
    frisby.create('Inserting a value into a channel.')
        .post(thisChannelResource, null, { body: messageText})
        .addHeader("Content-Type", "text/plain")
        .expectStatus(200)
        .expectHeader('content-type', 'application/json')
        .expectJSON('_links', {
            channel: {
                href: thisChannelResource
            }
        })
        .expectJSON('_links.self', {
            href: function (value) {
                var regex = new RegExp("^" + thisChannelResource.replace(/\//g, "\\/").replace(/\:/g, "\\:") + "\\/[A-Z,0-9]{16}$");
                expect(value).toMatch(regex);
            }
        })
        .expectJSON({
            id: function (value) {
                expect(value).toMatch(/^[A-Z,0-9]{16}$/);
            }
        })
        .afterJSON(function (result) {
            var valueUrl = result['_links']['self']['href'];
            console.log('Now attempting to fetch back my data from ' + valueUrl);
            frisby.create('Fetching value to ensure that it was inserted.')
                .get(valueUrl)
                .expectStatus(200)
                .expectHeader('content-type', 'text/plain')
                .expectBodyContains(messageText)
                .toss();
        })
        .inspectJSON()
        .toss();
});
