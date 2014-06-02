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
                headers: {"Content-Type": "application/json", user: 'somebody' },
                body: JSON.stringify({ "data": Date.now()})},
            function (err, response, body) {
                expect(err).toBeNull();
                expect(response.statusCode).toBe(responseCode);
                done();
            });
    });
}

function putGroup(groupName, groupConfig, status) {
    status = status || 201;
    var groupResource = groupUrl + "/" + groupName;
    it('creates group ' + groupName, function (done) {
        request.put({url : groupResource,
                headers : {"Content-Type" : "application/json"},
                body : JSON.stringify(groupConfig)},
            function (err, response, body) {
                expect(err).toBeNull();
                expect(response.statusCode).toBe(status);
                if (status === 201) {
                    expect(response.headers.location).toBe(groupResource);
                }
                if (typeof groupConfig !== "undefined" && status < 400) {
                    var parse = JSON.parse(body);
                    expect(parse.callbackUrl).toBe(groupConfig.callbackUrl);
                    expect(parse.channelUrl).toBe(groupConfig.channelUrl);
                    expect(parse.transactional).toBe(groupConfig.transactional);
                    expect(parse.name).toBe(groupName);
                }
                done();
            });
    });
    return groupResource;
}

function getGroup(groupName, groupConfig) {
    var groupResource = groupUrl + "/" + groupName;
    it('gets group ' + groupName, function (done) {
        request.get({url : groupResource,
                headers : {"Content-Type" : "application/json"} },
            function (err, response, body) {
                expect(err).toBeNull();
                expect(response.statusCode).toBe(200);
                var parse = JSON.parse(body);
                expect(parse._links.self.href).toBe(groupResource);
                if (typeof groupConfig !== "undefined") {
                    expect(parse.callbackUrl).toBe(groupConfig.callbackUrl);
                    expect(parse.channelUrl).toBe(groupConfig.channelUrl);
                    expect(parse.transactional).toBe(groupConfig.transactional);
                    expect(parse.name).toBe(groupName);
                }
                done();
            });
    });
    return groupResource;
}

function deleteGroup(groupName) {
    var groupResource = groupUrl + "/" + groupName;
    it('deletes the group ' + groupName, function (done) {
        request.del({url: groupResource },
            function (err, response, body) {
                expect(err).toBeNull();
                expect(response.statusCode).toBe(202);
                done();
            });
    });
}

function sleep(millis) {
    runs(function() {
        flag = false;

        setTimeout(function() {
            flag = true;
        }, millis);
    });

    waitsFor(function() {
        return flag;
    }, millis + 1000);
}

exports.runInTestChannel = runInTestChannel;
exports.download = download;
exports.randomChannelName = randomChannelName;
exports.configureFrisby = configureFrisby;
exports.runInTestChannelJson = runInTestChannelJson;
exports.createChannel = createChannel;
exports.addItem = addItem;
exports.sleep = sleep;
exports.putGroup = putGroup;
exports.getGroup = getGroup;
exports.deleteGroup = deleteGroup;

