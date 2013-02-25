var utils = require('./utils.js');
require('./integration_config.js');
var frisby = require('frisby');

var channelName = utils.randomChannelName();
var thisChannelResource = channelUrl + "/" + channelName;
var messageText = "";

utils.runInTestChannel(channelName, function () {

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
                .get(result['_links']['self']['href'])
                .expectStatus(200)
                .expectHeader('content-type', 'text/plain')
                .expectHeader('content-length', "0")
                .toss();
        })
        .inspectJSON()
        .toss();
});