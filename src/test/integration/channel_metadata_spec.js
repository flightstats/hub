require('./integration_config.js');

var channelName = utils.randomChannelName();
var thisChannelResource = channelUrl + "/" + channelName;
var messageText = "MY SUPER TEST CASE: this & <that>. " + Math.random().toString();
var testName = __filename;

utils.configureFrisby();

utils.runInTestChannel(testName, channelName, function () {
    frisby.create(testName + ': Inserting a value into a channel.')
        .post(thisChannelResource, null, { body : messageText})
        .addHeader("Content-Type", "text/plain")
        .expectStatus(201)
        .afterJSON(function (result) {
            var valueUrl = result['_links']['self']['href'];
            frisby.create(testName + ': Now fetching metadata')
                .get(thisChannelResource)
                .expectStatus(200)
                .expectHeader('content-type', 'application/json')
                .expectJSON('_links.latest', {
                    href : thisChannelResource + '/latest'
                })
                .expectJSON('_links.ws', {
                    href : thisChannelResource.replace(/^http/, "ws") + '/ws'
                })
                .expectJSON({
                    name : channelName,
                    ttlDays : 120
                })
                .toss();
        })
        .toss();
});
