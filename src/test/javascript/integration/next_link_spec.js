require('../integration_config');

var request = require('request');
var http = require('http');
var channelName = utils.randomChannelName();
var groupName = utils.randomChannelName();
var channelResource = channelUrl + "/" + channelName;
var testName = __filename;

describe(testName, function () {
    utils.createChannel(channelName);

    it('adds items and traverses next links', function (done) {
        var values = [];
        var items = [];
        utils.postItemQ(channelResource)
            .then(function (value) {
                values.push(value);
                items.push(value.body._links.self.href);
                return utils.getQ(value.body._links.self.href, 200);
            })
            .then(function (value) {
                return utils.getQ(items[0] + '/next', 404);
            })
            .then(function (value) {
                return utils.getQ(items[0] + '/next/2', 200);
            })
            .then(function (value) {
                expect(value.body._links.uris.length).toBe(0);
                return utils.postItemQ(channelResource);
            })
            .then(function (value) {
                items.push(value.body._links.self.href);
                return utils.postItemQ(channelResource);
            })
            .then(function (value) {
                items.push(value.body._links.self.href);
                return utils.getQ(items[0] + '/next', 200);
            })
            .then(function (value) {
                expect(value.response.request.href).toBe(items[1]);
                return utils.getQ(items[0] + '/next/2', 200, true);
            })
            .then(function (value) {
                expect(value.body._links.uris.length).toBe(0);
                expect(value.body._links.next).toBeUndefined();
                return utils.getQ(items[0] + '/next/2', 200);
            })
            .then(function (value) {
                expect(value.body._links.uris.length).toBe(2);
                expect(value.body._links.uris[0]).toBe(items[1]);
                expect(value.body._links.uris[1]).toBe(items[2]);
                expect(value.body._links.next.href).toBe(items[2] + '/next/2?stable=false');
                done();
            })
    });

});

