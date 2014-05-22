require('./integration_config.js');

var channelName = utils.randomChannelName();
var thisChannelResource = channelUrl + "/" + channelName;
var messageText = "MY SUPER TEST CASE: this & <that>. " + Math.random().toString();
var testName = "insert_and_fetch_round_trip_text_spec";

utils.configureFrisby();

utils.runInTestChannel(channelName, function () {
    frisby.create(testName + ': Inserting a value into a channel.')
        .post(thisChannelResource, null, { body: messageText})
        .addHeader("Content-Type", "text/plain")
        .expectStatus(201)
        .expectHeader('content-type', 'application/json')
        .expectJSON('_links', {
            channel: {
                href: thisChannelResource
            }
        })
        .expectJSON('_links.self', {
            href: function (value) {
                var regex = new RegExp("^" + thisChannelResource.replace(/\//g, "\\/").replace(/\:/g, "\\:") + "\\/1000");
                expect(value).toMatch(regex);
            }
        })
        .afterJSON(function (result) {
            var valueUrl = result['_links']['self']['href'];
            frisby.create(testName + ': Fetching value to ensure that it was inserted.')
                .get(valueUrl)
                .expectStatus(200)
                .expectHeader('content-type', 'text/plain')
                .expectBodyContains(messageText)
                .after(function (err, res, body) {
                    expect(res.headers['user']).toBeUndefined();
                })
                .toss();
        })
        .toss();
});
