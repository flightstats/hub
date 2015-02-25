require('./integration_config.js');

var request = require('request');
var http = require('http');
var Q = require('q');
var channelName = utils.randomChannelName();
var groupName = utils.randomChannelName();
var channelResource = channelUrl + "/" + channelName;
var testName = __filename;

describe(testName, function () {
    utils.createChannel(channelName);

    console.log('channelName', channelName);

    it('adds items and traverses next links', function (done) {
        var values = [];
        var items = [];
        utils.postItemQ(channelResource)
            .then(function (value) {
                values.push(value);
                var item_link = value.body._links.self.href;
                items.push(item_link);
                console.log('item_link', item_link);
                return utils.getQ(value.body._links.self.href + '/next/10');
            })
            .then(function (value) {
                console.log('value', value.body);
                expect(value.body._links.uris.length).toBe(0);
                expect(value.body._links.previous).not.toBeUndefined();
                done();
            })
    });

});

