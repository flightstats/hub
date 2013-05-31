require('./integration_config.js');

var channelName = utils.randomChannelName();

utils.configureFrisby();

utils.runInTestChannel(channelName, function (channelResponse) {
    var channelResource = channelResponse['_links']['self']['href'];
    frisby.create('Inserting a first item')
        .post(channelResource, null, { body: "FIRST ITEM"})
        .addHeader("Content-Type", "text/plain")
        .expectStatus(201)
        .afterJSON(function (response) {
            var firstItemUrl = response['_links']['self']['href'];
            frisby.create('Verifying that first channel item doesnt have a next')
                .get(firstItemUrl)
                .expectStatus(200)
                .after(function (err, res, body) {
                    for (var item in res.headers) {
                        if (item == "link") {
                            expect(res.headers[item]).not.toContain("next");
                        }
                    }
                    frisby.create("Inserting a second item")
                        .post(channelResource, null, {body: "SECOND ITEM"})
                        .addHeader("Content-Type", "text/plain")
                        .expectStatus(201)
                        .afterJSON(function (response) {
                            var secondItemUrl = response['_links']['self']['href'];
                            frisby.create("Checking the Link header that should come back with the first url now.")
                                .get(firstItemUrl)
                                .expectStatus(200)
                                .expectHeader("link", "<" + secondItemUrl + ">;rel=\"next\"")
                                .toss()
                        })
                        .toss();
                })
                .toss();
        })
        .toss();
});

