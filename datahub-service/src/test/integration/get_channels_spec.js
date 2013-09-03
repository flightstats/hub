require('./integration_config.js');

var channelName = utils.randomChannelName();

utils.configureFrisby();

frisby.create('Making a channel and then checking that it is in the result list...')
    .post(channelUrl, null, { body: JSON.stringify({ "name": channelName})})
    .addHeader("Content-Type", "application/json")
    .afterJSON(function () {
        frisby.create("Fetching the channel list")
            .get(channelUrl)
            .expectStatus(200)
            .expectHeader('content-type', 'application/json')
            .afterJSON(function (result) {
                var selfLink = result['_links']['self']['href'];
                expect(selfLink).toBe(channelUrl);
                var channels = result['_links']['channels'];
                var foundHref = "";
                for (var i = 0; i < channels.length; i++) {
                    if (channels[i]['name'] == channelName) {
                        foundHref = channels[i]['href'];
                    }
                }
                expect(foundHref).toBe(channelUrl + "/" + channelName);

            }).toss()
    })
    .toss();

