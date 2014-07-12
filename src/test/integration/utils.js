require('./integration_config.js');
var frisby = require('frisby');
var http = require('http');
var https = require('https');
var fs = require('fs');
var request = require('request');

function runInTestChannel(testName, channelName, functionToExecute) {
    testName = testName || '';
    runInTestChannelJson(testName, JSON.stringify({ "name" : channelName}), functionToExecute);
}

function runInTestChannelJson(testName, jsonBody, functionToExecute) {
    frisby.create('Creating channel ' + testName + ' ' + jsonBody)
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
    timeout = typeof timeout !== 'undefined' ? timeout : 30000;
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
    it("adds item to " + url, function (done) {
        postItem(url, responseCode, done);
    });
}

function postItem(url, responseCode, completed) {
    responseCode = responseCode || 201;
    completed = completed || function () {};
    request.post({url : url,
            headers : {"Content-Type" : "application/json", user : 'somebody' },
            body : JSON.stringify({ "data" : Date.now()})},
        function (err, response, body) {
            expect(err).toBeNull();
            expect(response.statusCode).toBe(responseCode);
            completed();
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

function getGroup(groupName, groupConfig, status) {
    var groupResource = groupUrl + "/" + groupName;
    status = status || 200;
    it('gets group ' + groupName, function (done) {
        request.get({url : groupResource,
                headers : {"Content-Type" : "application/json"} },
            function (err, response, body) {
                expect(err).toBeNull();
                expect(response.statusCode).toBe(status);
                if (status < 400) {
                    var parse = JSON.parse(body);
                    expect(parse._links.self.href).toBe(groupResource);
                    if (typeof groupConfig !== "undefined") {
                        expect(parse.callbackUrl).toBe(groupConfig.callbackUrl);
                        expect(parse.channelUrl).toBe(groupConfig.channelUrl);
                        expect(parse.transactional).toBe(groupConfig.transactional);
                        expect(parse.name).toBe(groupName);
                    }
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

function startServer(port, callback) {
    callback = callback || function () {};
    var started = false;
    runs(function () {
        server = http.createServer(function (request, response) {
            request.on('data', function(chunk) {
                callback(chunk.toString());
            });
            response.writeHead(200);
            response.end();
        });

        server.on('connection', function(socket) {
            socket.setTimeout(1000);
        });

        server.listen(port, function () {
            started = true;
        });
    });

    waitsFor(function() {
        return started;
    }, 11000);
}

function startHttpsServer(port, callback, done) {

    var options = {
        key: fs.readFileSync('localhost.key'),
        cert: fs.readFileSync('localhost.cert')
    };

    callback = callback || function () {};
    var server = https.createServer(options, function (request, response) {
        request.on('data', function(chunk) {
            callback(chunk.toString());
        });
        response.writeHead(200);
        response.end();
    });

    server.on('connection', function(socket) {
        socket.setTimeout(1000);
    });

    server.listen(port, function () {
        done();
    });

    return server;
}

function closeServer(callback) {
    callback = callback || function () {};
    var closed = false;
    runs(function () {
        server.close(function () {
            closed = true;
        });

        callback();
    });

    waitsFor(function() {
        return closed;
    }, 13000);
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
exports.postItem = postItem;
exports.startServer = startServer;
exports.startHttpsServer = startHttpsServer;
exports.closeServer = closeServer;

