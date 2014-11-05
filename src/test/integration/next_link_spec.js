require('./../integration/integration_config.js');

var request = require('request');
var http = require('http');
var Q = require('q');
var channelName = utils.randomChannelName();
var groupName = utils.randomChannelName();
var channelResource = channelUrl + "/" + channelName;
var testName = __filename;
var port = callbackPort + 2;
var callbackUrl = callbackDomain + ':' + port + '/';
var groupConfig = {
    callbackUrl : callbackUrl,
    channelUrl : channelResource
};

describe(testName, function () {
    console.log('channelName ' + channelName);
    utils.createChannel(channelName);

    it('adds items and traverses links', function (done) {
        var values = [];
        var items = [];
        postItem(channelResource)
            .then(function (value) {
                values.push(value);
                items.push(value.body._links.self.href);
                return getItem(value.body._links.self.href, 200);
            })
            .then(function (value) {
                return getItem(items[0] + '/next', 404);
            })
            .then(function (value) {
                return getItem(items[0] + '/previous', 404);
            })
            .then(function (value) {
                return postItem(channelResource);
            })
            .then(function (value) {
                items.push(value.body._links.self.href);
                return getItem(items[0] + '/next', 200);
            })
            .then(function (value) {
                expect(value.response.request.href).toBe(items[1]);
                done();
            })
    });

    function postItem(url) {
        var deferred = Q.defer();
        request.post({url : url, json : true,
                headers : {"Content-Type" : "application/json" },
                body : JSON.stringify({ "data" : Date.now()})},
            function (err, response, body) {
                expect(err).toBeNull();
                expect(response.statusCode).toBe(201);
                deferred.resolve({response : response, body : body});
            });
        return deferred.promise;
    }

    function getItem(url, status) {
        status = status || 200;
        var deferred = Q.defer();
        request.get({url : url, json : true },
            function (err, response, body) {
                expect(err).toBeNull();
                expect(response.statusCode).toBe(status);
                deferred.resolve({response : response, body : body});
            });
        return deferred.promise;
    }

});

