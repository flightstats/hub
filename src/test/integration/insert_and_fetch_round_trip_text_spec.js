var utils = require('./utils.js');
var frisby = require('frisby');

var channelName = "integrationtests";
var thisChannelResource = channelUrl + "/" + channelName;
var messageText = "MY SUPER TEST CASE: this & <that>. " + Math.random().toString();

utils.ensureTestChannelCreated(channelName);

console.info('Inserting a value...');
frisby.create('Inserting a value into a channel.')
    .post(thisChannelResource, null, { body: messageText})
    .addHeader("Content-Type", "text/plain")
    .expectStatus(200)
    .expectHeader('content-type', 'application/json')
    .expectJSON({
        _links: {
            channel: {
                href: thisChannelResource 
            }
			//TOOD: Validate the value "self" url
        },
        //TODO: validate the id
    })
    .afterJSON(function (result) {
		var valueUrl = result['_links']['self']['href'];
		console.log('Now attempting to fetch back my data from ' + valueUrl);
        frisby.create('Fetching value to ensure that it was inserted.')
            .get(valueUrl)
            .expectStatus(200)
			.expectHeader('content-type', 'application/octet-stream')
			.expectBodyContains(messageText)
            .toss();
    })
    .inspectJSON()
    .toss();

