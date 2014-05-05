require('./integration_config.js');
var frisby = require('frisby');
var http = require('http');
var fs = require('fs');
var request = require('request');

function runInTestChannel(channelName, functionToExecute) {
    runInTestChannelJson(JSON.stringify({ "name": channelName}), functionToExecute);
}

function runInTestChannelJson(jsonBody, functionToExecute) {
    frisby.create('Ensuring that the test channel exists. ' + jsonBody)
        .post(channelUrl, null, { body: jsonBody})
        .addHeader("Content-Type", "application/json")
        .expectStatus(201)
        .afterJSON(functionToExecute)
        .toss();
}

function randomChannelName() {
    return "test_" + Math.random().toString().replace(".", "_");
}

function download(url, completionHandler) {
    http.get(url, function (res) {
        var imagedata = '';
        res.setEncoding('binary');

        res.on('data', function (chunk) {
            imagedata += chunk
        });

        res.on('end', function () {
            completionHandler(imagedata);
        });
    });
}

function configureFrisby(timeout) {
    timeout = typeof timeout !== 'undefined' ? timeout : 10000
    frisby.globalSetup({
        timeout: timeout
    });
}

function createChannel(channelName, url) {
    url = url || channelUrl;
    it("creates channel " + channelName + " at " + url, function (done) {
        request.post({url: url,
                headers: {"Content-Type": "application/json"},
                body: JSON.stringify({ "name": channelName })},
            function (err, response, body) {
                expect(err).toBeNull();
                expect(response.statusCode).toBe(201);
                done();
            });
    });

}

function addItem(url, responseCode) {
    responseCode = responseCode || 201;
    it("adds item to " + url, function (done) {
        request.post({url: url,
                headers: {"Content-Type": "application/json"},
                body: JSON.stringify({ "data": Date.now()})},
            function (err, response, body) {
                expect(err).toBeNull();
                expect(response.statusCode).toBe(responseCode);
                done();
            });
    });
}

exports.runInTestChannel = runInTestChannel;
exports.download = download;
exports.randomChannelName = randomChannelName;
exports.configureFrisby = configureFrisby;
exports.runInTestChannelJson = runInTestChannelJson;
exports.createChannel = createChannel;
exports.addItem = addItem;

