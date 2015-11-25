require('./integration_config.js');
var request = require('request');

var channelName = utils.randomChannelName();
var thisChannelResource = channelUrl + "/" + channelName;
var messageText = "MY SUPER TEST CASE: this & <that>. " + Math.random().toString();
var testName = __filename;

utils.configureFrisby();

utils.runInTestChannel(testName, channelName, function () {
    // Note: We have to use request directly here, because Frisby insists on having a content-type specified.
    request.post({url : thisChannelResource, body : messageText}, function (error, response, body) {
        expect(error).toBeNull();
        resultObj = utils.parseJson(response, testName);
        expect(resultObj['_links']['channel']['href']).toBe(thisChannelResource);
        var valueUrl = resultObj['_links']['self']['href'];
        frisby.create(testName + ': Fetching data and checking content-type header.')
            .get(valueUrl)
            .expectStatus(200)
            .expectHeader('content-type', 'application/octet-stream')
            .expectBodyContains(messageText)
            .toss();
    });
});