require('./integration_config.js');
var frisby = require('frisby');
var http = require('http');
var fs = require('fs');

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

exports.runInTestChannel = runInTestChannel;
exports.download = download;
exports.randomChannelName = randomChannelName;
exports.configureFrisby = configureFrisby;
exports.runInTestChannelJson = runInTestChannelJson;
