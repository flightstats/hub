var utils = require('./utils.js');
var frisby = require('frisby');
var request = require('request');

var channelName = utils.randomChannelName();
var thisChannelResource = channelUrl + "/" + channelName;
var messageText = "MY SUPER TEST CASE: this & <that>. " + Math.random().toString();

utils.runInTestChannel(channelName, function () {

    console.info('Inserting a value...');
    // Note: We have to use request directly here, because Frisby insists on having a content-type specified.
    request.post({url: thisChannelResource, body: messageText}, function (error, response, body) {
        expect(error).toBeNull();
        resultObj = JSON.parse(body);
        expect(resultObj['_links']['channel']['href']).toBe(thisChannelResource);
        var valueUrl = resultObj['_links']['self']['href'];
        console.log('Now attempting to fetch back my data from ' + valueUrl);
        frisby.create('Fetching data and checking content-type header.')
            .get(valueUrl)
            .expectStatus(200)
            .expectHeader('content-type', 'application/octet-stream')
            .expectBodyContains(messageText)
            .toss();
    });
});