require('../integration_config');

var request = require('request');
var http = require('http');
var channelName = utils.randomChannelName();
var groupName = utils.randomChannelName();
var channelResource = channelUrl + "/" + channelName;
var testName = __filename;

describe(testName, function () {
    utils.createChannel(channelName);

    console.log('channelName', channelName);

    it('adds item and checks relative links', function (done) {
        var item_href;
        utils.postItemQ(channelResource)
            .then(function (value) {
                item_href = value.body._links.self.href;
                console.log('item_link', item_href);
                return utils.getQ(item_href + '/next/10');
            })
            .then(function (value) {
                expect(value.body._links.uris.length).toBe(0);
                expect(value.body._links.previous).not.toBeUndefined();
                expect(value.body._links.previous.href).toBe(item_href + '/previous/10?stable=false');
                done();
            })
    });

});

