require('../integration_config');

var request = require('request');
var http = require('http');
var channelName = utils.randomChannelName();
var groupName = utils.randomChannelName();
var channelResource = channelUrl + "/" + channelName;
var testName = __filename;

describe(testName, function () {
    utils.putChannel(channelName, function () {
    }, {"name": channelName, ttlDays: 1});

    it('adds items and traverses previous links ' + channelName, function (done) {
        var values = [];
        var items = [];
        utils.postItemQ(channelResource)
            .then(function (value) {
                values.push(value);
                items.push(value.body._links.self.href);
                return getItem(value.body._links.self.href, 200, '0');
            })
            .then(function (value) {
                return getItem(items[0] + '/previous', 404, 'A');
            })
            .then(function (value) {
                return getItem(items[0] + '/previous/2', 200, 'B');
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
                return getItem(items[2] + '/previous', 200, 'C');
            })
            .then(function (value) {
                expect(value.response.request.href).toBe(items[1]);
                return getItem(items[2] + '/previous/2', 200);
            })
            .then(function (value) {
                expect(value.body._links.uris.length).toBe(2);
                expect(value.body._links.uris[0]).toBe(items[0]);
                expect(value.body._links.uris[1]).toBe(items[1]);
                expect(value.body._links.previous.href).toBe(items[0] + '/previous/2?stable=false');
                done();
            })
    }, 2 * 60001);

    function getItem(url, status) {
        status = status || 200;
        var promise = new Promise();
        request.get({url : url + '?stable=false', json : true },
            function (err, response, body) {
                expect(err).toBeNull();
                expect(response.statusCode).toBe(status);
                promise.resolve({response : response, body : body});
            });
        return promise;
    }

});

