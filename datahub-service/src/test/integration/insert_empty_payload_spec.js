require('./integration_config.js');

var channelName = utils.randomChannelName();
var thisChannelResource = channelUrl + "/" + channelName;
var messageText = "";

utils.configureFrisby();

utils.runInTestChannel(channelName, function () {
    frisby.create('Inserting a value into a channel.')
        .post(thisChannelResource, null, { body: messageText})
        .addHeader("Content-Type", "text/plain")
        .expectStatus(201)
        .expectHeader('content-type', 'application/json')
        .expectJSON({
            _links: {
                channel: {
                    href: thisChannelResource
                }
                //TOOD: Validate the value "self" url
            }
        })
        .afterJSON(function (result) {
            var valueUrl = result['_links']['self']['href'];
            frisby.create('Fetching value to ensure that it was inserted.')
                .get(valueUrl)
                .expectStatus(200)
                .expectHeader('content-type', 'text/plain')
                .expectHeader('content-length', "0")
                .toss();
        })
        .toss();
});