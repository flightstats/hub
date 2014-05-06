require('./integration_config.js');

var channelName = utils.randomChannelName();
var tag = Math.random().toString().replace(".", "A");
var jsonBody = JSON.stringify({ "name": channelName, "tags": [ tag]});
var channelResource = channelUrl + "/" + channelName;
var tagUrl = hubUrlBase + "/tag";
var testName = 'get_tags_spec';
var foundTagHref = "";

utils.configureFrisby();

frisby.create(testName + ' Test tag resources for new channel')
    .post(channelUrl, null, { body: jsonBody})
    .addHeader("Content-Type", "application/json")
    .expectStatus(201)
    .expectJSON({"tags": [tag]})
    .afterJSON(function (result) {
        frisby.create(testName + ': getting all tags')
            .get(tagUrl)
            .expectStatus(200)
            .expectHeader('content-type', 'application/json')
            .afterJSON(function (result) {
                var selfLink = result['_links']['self']['href'];
                expect(selfLink).toBe(tagUrl);
                var tags = result['_links']['tags'];
                for (var i = 0; i < tags.length; i++) {
                    if (tags[i]['name'] == tag) {
                        foundTagHref = tags[i]['href'];
                    }
                }
                expect(foundTagHref).toBe(tagUrl + "/" + tag);
            })
            .after(function () {
                frisby.create(testName + ': getting specific tag resource ' + foundTagHref)
                    .get(foundTagHref)
                    .expectStatus(200)
                    .expectHeader('content-type', 'application/json')
                    .afterJSON(function (result) {
                        var selfLink = result['_links']['self']['href'];
                        expect(selfLink).toBe(foundTagHref);
                        var channels = result['_links']['channels'];
                        var channelHref = "";
                        for (var i = 0; i < channels.length; i++) {
                            if (channels[i]['name'] == channelName) {
                                channelHref = channels[i]['href'];
                            }
                        }
                        expect(channelHref).toBe(channelUrl + "/" + channelName);
                    })
                    .after(function () {
                        frisby.create(testName + 'delete channel ' + channelResource)
                            .delete(channelResource)
                            .expectStatus(202)
                            .toss()
                    })
                    .toss()
            })
            .toss()
    })
    .toss();


