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

    var items = [];
    var headers = {'Content-Type': 'application/json'};
    var body = {'name': channelName};

    function postOneItem(done) {
        utils.httpPost(channelResource, headers, body)
            .then(function (response) {
                expect(response.statusCode).toEqual(201);
                items.push(response.body._links.self.href);
            })
            .catch(error => expect(error).toBeNull())
            .finally(done);
    }

    it('posts item', function (done) {
        postOneItem(done);
    });

    it('gets 404 from /previous ', function (done) {
        utils.httpGet(items[0] + '/previous', headers, body)
            .then(function (response) {
                expect(response.statusCode).toEqual(404);
            })
            .catch(error => expect(error).toBeNull())
            .finally(done);
    });

    it('gets empty list from /previous/2 ', function (done) {
        utils.httpGet(items[0] + '/previous/2', headers, body)
            .then(function (response) {
                expect(response.statusCode).toEqual(200);
                expect(response.body._links.uris.length).toBe(0);
            })
            .catch(error => expect(error).toBeNull())
            .finally(done);
    });

    it('posts item', function (done) {
        postOneItem(done);
    });

    it('posts item', function (done) {
        postOneItem(done);
    });

    it('gets item from /previous ', function (done) {
        utils.httpGet(items[2] + '/previous?stable=false', headers, body)
            .then(function (response) {
                expect(response.statusCode).toEqual(303);
                expect(response.headers.location).toBe(items[1]);
            })
            .catch(error => expect(error).toBeNull())
            .finally(done);
    });

    it('gets items from /previous/2 ', function (done) {
        utils.httpGet(items[2] + '/previous/2?stable=false', headers, body)
            .then(function (response) {
                expect(response.statusCode).toEqual(200);
                expect(response.body._links.uris.length).toBe(2);
                expect(response.body._links.uris[0]).toBe(items[0]);
                expect(response.body._links.uris[1]).toBe(items[1]);
            })
            .catch(error => expect(error).toBeNull())
            .finally(done);
    });

    it('gets inclusive items from /previous/2 ', function (done) {
        utils.httpGet(items[2] + '/previous/2?stable=false&inclusive=true', headers, body)
            .then(function (response) {
                expect(response.statusCode).toEqual(200);
                expect(response.body._links.uris.length).toBe(2);
                expect(response.body._links.uris[0]).toBe(items[1]);
                expect(response.body._links.uris[1]).toBe(items[2]);
            })
            .catch(error => expect(error).toBeNull())
            .finally(done);
    });

});

