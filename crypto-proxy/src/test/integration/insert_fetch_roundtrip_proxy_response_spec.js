require('./integration_config.js');

var channelName = utils.randomChannelName();
var channelResource = channelUrl + "/" + channelName;
var channelCryptoResource = cyptoChannelUrl + "/" + channelName;
var messageText = "MY SUPER TEST CASE: this & <that>. " + Math.random().toString();
var testName = 'insert_fetch_roundtrip_proxy_response_spec';

utils.configureFrisby();

utils.runInTestChannel(channelName, function () {
    frisby.create(testName + ': Inserting a value into a channel.')
        .post(channelCryptoResource, null, { body: messageText})
        .addHeader("Content-Type", "text/plain")
        .expectStatus(201)
        .expectHeader('content-type', 'application/json')
        .expectJSON('_links', {
            channel: {
                href: channelResource
            }
        })
        .expectJSON('_links.self', {
            href: function (value) {
                var regex = new RegExp("^" + channelResource.replace(/\//g, "\\/").replace(/\:/g, "\\:") + "\\/[A-Z,0-9]{16}$");
                expect(value).toMatch(regex);
            }
        })
        .afterJSON(function (result) {
            var valueUrl = result['_links']['self']['href'];
            valueUrl = valueUrl.replace(channelResource, channelCryptoResource);
            frisby.create(testName + ': Fetching value to ensure that it was inserted.')
                .get(valueUrl)
                .expectStatus(200)
                .expectHeader('content-type', 'text/plain')
                .expectBodyContains(messageText)
                .toss();
        })
        .toss();
});
