require('./integration_config.js');

var request = require('request');
var http = require('http');
var Q = require('q');
var moment = require('moment');
var channelName = utils.randomChannelName();
var groupName = utils.randomChannelName();
var channelResource = channelUrl + "/" + channelName;
var testName = __filename;

describe(testName, function () {
    utils.putChannel(channelName, function () {
    }, {"name": channelName, ttlDays: 1});

    function time(text) {
        console.log(moment().format('h:mm:ss.SSS'), text);
    }

    it('adds items and traverses previous links', function (done) {
        var values = [];
        var items = [];
        time('starting');
        utils.postItemQ(channelResource)
            .then(function (value) {
                values.push(value);
                items.push(value.body._links.self.href);
                time('getting self');
                return getItem(value.body._links.self.href, 200, '0');
            })
            .then(function (value) {
                time('getting previousA');
                return getItem(items[0] + '/previous', 404, 'A');
            })
            .then(function (value) {
                time('getting previousA2');
                return getItem(items[0] + '/previous/2', 200, 'B');
            })
            .then(function (value) {
                time('posting1');
                expect(value.body._links.uris.length).toBe(0);
                return utils.postItemQ(channelResource);
            })
            .then(function (value) {
                time('posting2');
                items.push(value.body._links.self.href);
                return utils.postItemQ(channelResource);
            })
            .then(function (value) {
                items.push(value.body._links.self.href);
                time('getting previousB');
                return getItem(items[2] + '/previous', 200, 'C');
            })
            .then(function (value) {
                time('getting previousB2');
                expect(value.response.request.href).toBe(items[1]);
                return getItem(items[2] + '/previous/2', 200);
            })
            .then(function (value) {
                time('verifying');
                expect(value.body._links.uris.length).toBe(2);
                expect(value.body._links.uris[0]).toBe(items[0]);
                expect(value.body._links.uris[1]).toBe(items[1]);
                expect(value.body._links.previous.href).toBe(items[0] + '/previous/2?stable=false');
                done();
            })
    }, 2 * 60001);

    function getItem(url, status) {
        status = status || 200;
        var deferred = Q.defer();
        request.get({url: url + '?stable=false', json: true},
            function (err, response, body) {
                expect(err).toBeNull();
                expect(response.statusCode).toBe(status);
                deferred.resolve({response: response, body: body});
            });
        return deferred.promise;
    }

});

