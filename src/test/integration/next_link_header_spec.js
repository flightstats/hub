require('./integration_config.js');

var channelName = utils.randomChannelName();
var testName = __filename;

utils.configureFrisby();

utils.runInTestChannel(testName, channelName, function (channelResponse) {
    var channelResource = channelResponse['_links']['self']['href'];
    frisby.create(testName + ': Inserting a first item')
        .post(channelResource, null, { body : "FIRST ITEM"})
        .addHeader("Content-Type", "text/plain")
        .expectStatus(201)
        .afterJSON(function (response) {
            var firstItemUrl = response['_links']['self']['href'];
            frisby.create(testName + ': Verifying that first channel item doesnt have a next')
                .get(firstItemUrl)
                .expectStatus(200)
                .after(function (err, res, body) {

                    frisby.create(testName + ": Inserting a second item")
                        .post(channelResource, null, {body : "SECOND ITEM"})
                        .addHeader("Content-Type", "text/plain")
                        .expectStatus(201)
                        .afterJSON(function (response) {
                            var secondItemUrl = response['_links']['self']['href'];
                            frisby.create(testName + ": Checking the Link header that should come back with the first url now.")
                                .get(firstItemUrl)
                                .expectStatus(200)
                                .toss()
                        })
                        .toss();
                })
                .toss();
        })
        .toss();
});

