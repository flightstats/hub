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

    it('adds items and traverses next links', function (done) {
        var values = [];
        var items = [];
        utils.postItemQ(channelResource)
            .then(function (value) {
                values.push(value);
                items.push(value.body._links.self.href);
                return getItem(value.body._links.self.href, 200);
            })
            .then(function (value) {
                return getItem(items[0] + '/next', 404);
            })
            .then(function (value) {
                return getItem(items[0] + '/next/2', 200);
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
                return getItem(items[0] + '/next', 200);
            })
            .then(function (value) {
                expect(value.response.request.href).toBe(items[1]);
                return getItem(items[0] + '/next/2', 200);
            })
            .then(function (value) {
                expect(value.body._links.uris.length).toBe(2);
                expect(value.body._links.uris[0]).toBe(items[1]);
                expect(value.body._links.uris[1]).toBe(items[2]);
                expect(value.body._links.next.href).toBe(items[2] + '/next/2');
                done();
            })
    });

    function getItem(url, status) {
        status = status || 200;
        var deferred = Q.defer();
        request.get({url : url + '?stable=false', json : true },
            function (err, response, body) {
                expect(err).toBeNull();
                expect(response.statusCode).toBe(status);
                deferred.resolve({response : response, body : body});
            });
        return deferred.promise;
    }

});

