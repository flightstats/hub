require('./integration_config.js');
var frisby = require('frisby');

var channelName = "integrationtests";
var jsonBody = JSON.stringify({ "name": channelName});
var thisChannelResource = channelUrl + "/" + channelName;
var messageText = "";

console.info('Ensuring that test channel has been created...');
frisby.create('Ensuring that the test channel exists.')
    .post(channelUrl, null, { body: JSON.stringify({ "name": channelName})})
    .addHeader("Content-Type", "application/json")
    .toss();

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

