var utils = require('./utils.js');
var frisby = require('frisby');

var channelName = "integrationtests";
var thisChannelResource = channelUrl + "/" + channelName;
var messageText = "MY SUPER TEST CASE: this & <that>. " + Math.random().toString();

utils.ensureTestChannelCreated(channelName);

console.info('Inserting a value...');
frisby.create('Checking that the content-type is returned.')
    .post(thisChannelResource, null, { body: messageText})
    .addHeader("Content-Type", "application/fractals")
    .expectStatus(200)
    .expectHeader('content-type', 'application/json')
    .expectJSON('_links', {
        channel: {
            href: thisChannelResource
        }
    })
    .afterJSON(function (result) {
        var valueUrl = result['_links']['self']['href'];
        console.log('Now attempting to fetch back my data from ' + valueUrl);
        frisby.create('Fetching data and checking content-type header.')
            .get(valueUrl)
            .expectStatus(200)
            .expectHeader('content-type', 'application/fractals')
            .expectBodyContains(messageText)
            .toss();
    })
    .inspectJSON()
    .toss();

