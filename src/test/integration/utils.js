require('./integration_config.js');
var frisby = require('frisby');
var http = require('http');
var https = require('https');
var fs = require('fs');
var request = require('request');
var Q = require('q');

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

function createChannel(channelName, url, description) {
    description = description || 'none';
    url = url || channelUrl;
    it("creates channel " + channelName + " at " + url, function (done) {
        console.log('creating channel ' + channelName + ' for ' + description);
        request.post({url: url,
                headers: {"Content-Type": "application/json"},
                body: JSON.stringify({ "name": channelName })},
            function (err, response, body) {
                expect(err).toBeNull();
                expect(response.statusCode).toBe(201);
                done();
            });
    }, 10 * 1001);

}

exports.putChannel = function putChannel(channelName, verify, body, description, expectedStatus) {
    expectedStatus = expectedStatus || 201;
    verify = verify || function () {};
    body = body || {"name" : channelName};
    description = description || 'none';
    it("puts channel " + channelName + " at " + channelUrl + ' ' + description, function (done) {
        var url = channelUrl + '/' + channelName;
        console.log('creating channel ' + channelName + ' for ' + description);
        request.put({
                url: url,
                headers: {"Content-Type": "application/json"},
                body: JSON.stringify(body)},
            function (err, response, body) {
                expect(err).toBeNull();
                expect(response.statusCode).toBe(expectedStatus);
                console.log("respinse " + body)
                verify(response, body);
                done();
            });
    });
}

function getChannel(channelName, verify, description, hubUrl) {
    verify = verify || function () { };
    description = description || 'none';
    hubUrl = hubUrl || hubUrlBase;
    it("gets channel " + channelName, function (done) {
        var url = hubUrl + '/channel/' + channelName;
        console.log('get channel ' + url + ' for ' + description);
        request.get({
                url: url,
                headers: {"Content-Type": "application/json"}
            },
            function (err, response, body) {
                expect(err).toBeNull();
                expect(response.statusCode).toBe(200);
                console.log("get response " + body)
                verify(response, body, hubUrl);
                done();
            });
    });
}

function addItem(url, responseCode) {
    it("adds item to " + url, function (done) {
        postItem(url, responseCode, done);
    }, 5099);
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
            console.log('posted', response.headers.location);
            completed();
        });
}

function postItemQ(url) {
    var deferred = Q.defer();
    request.post({
            url : url, json : true,
            headers : {"Content-Type" : "application/json"},
            body : JSON.stringify({"data" : Date.now()})
        },
        function (err, response, body) {
            expect(err).toBeNull();
            expect(response.statusCode).toBe(201);
            deferred.resolve({response : response, body : body});
        });
    return deferred.promise;
}

function getGroupUrl() {
    if (Math.random() > 0.5) {
        return hubUrlBase + '/webhook';
    }
    return hubUrlBase + '/group';
}

function putGroup(groupName, groupConfig, status, description, groupUrl) {
    description = description || 'none';
    status = status || 201;
    groupUrl = groupUrl || getGroupUrl();
    var groupResource = groupUrl + "/" + groupName;
    it('creates group ' + groupName, function (done) {
        console.log('creating group ' + groupName + ' for ' + description);
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
                    var parse = utils.parseJson(response, description);
                    expect(parse.callbackUrl).toBe(groupConfig.callbackUrl);
                    expect(parse.channelUrl).toBe(groupConfig.channelUrl);
                    expect(parse.name).toBe(groupName);
                }
                done();
            });
    });
    return groupResource;
}

function getGroup(groupName, groupConfig, status, verify) {
    var groupResource = getGroupUrl() + "/" + groupName;
    status = status || 200;
    verify = verify || function (parse) {
            if (typeof groupConfig !== "undefined") {
                expect(parse.callbackUrl).toBe(groupConfig.callbackUrl);
                expect(parse.channelUrl).toBe(groupConfig.channelUrl);
                expect(parse.transactional).toBe(groupConfig.transactional);
                expect(parse.name).toBe(groupName);
                expect(parse.batch).toBe(groupConfig.batch);
                if (groupConfig.ttlMinutes) {
                    expect(parse.ttlMinutes).toBe(groupConfig.ttlMinutes);
                }
                if (groupConfig.maxWaitMinutes) {
                    expect(parse.maxWaitMinutes).toBe(groupConfig.maxWaitMinutes);
                }
            }
        };
    utils.itSleeps(500);
    it('gets group ' + groupName, function (done) {
        request.get({url : groupResource,
                headers : {"Content-Type" : "application/json"} },
            function (err, response, body) {
                expect(err).toBeNull();
                expect(response.statusCode).toBe(status);
                if (response.statusCode < 400) {
                    var parse = utils.parseJson(response, groupName);
                    expect(parse._links.self.href).toBe(groupResource);
                    verify(parse);
                }
                done();
            });
    });
    return groupResource;
}

function deleteGroup(groupName) {
    var groupResource = getGroupUrl() + "/" + groupName;
    it('deletes the group ' + groupName, function (done) {
        request.del({url: groupResource },
            function (err, response, body) {
                expect(err).toBeNull();
                expect(response.statusCode).toBe(202);
                done();
            });
    }, 60 * 1000);
}

function itRefreshesChannels() {
    it('refreshes channels', function (done) {
        request.get(hubUrlBase + '/internal/channel/refresh',
            function (err, response, body) {
                expect(err).toBeNull();
                expect(response.statusCode).toBe(200);
                console.log('refresh', body);
                done();
            });
    });
}

function getQ(url, status, stable) {
    status = status || 200;
    stable = stable || false;
    var deferred = Q.defer();
    request.get({url: url + '?stable=' + stable, json: true},
        function (err, response, body) {
            expect(err).toBeNull();
            expect(response.statusCode).toBe(status);
            deferred.resolve({response: response, body: body});
        });
    return deferred.promise;
}

exports.itSleeps = function itSleeps(millis) {
    it('sleeps', function () {
        utils.sleep(millis);
    })
}

exports.sleep = function sleep(millis) {
    runs(function() {
        console.log('sleeping for ' + millis);
        flag = false;

        setTimeout(function() {
            flag = true;
        }, millis);
    });

    waitsFor(function() {
        return flag;
    }, millis + 1000);
}

exports.sleepQ = function sleepQ(millis) {
    var deferred = Q.defer();
    setTimeout(function () {
        deferred.resolve('slept');
    }, millis);
    return deferred.promise;
}

function timeout(millis) {
    it('waits for ' + millis, function (done) {
        setTimeout(function () {
            done()
        }, millis);
    });
}

function getPort() {
    var port = callbackPort++;
    console.log('using port', port)
    return port;
}

function startServer(port, callback) {
    console.log('starting server ' + port);
    var started = false;
    runs(function () {
        server = http.createServer(function (request, response) {
            serverResponse(request, response, callback);
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

function serverResponse(request, response, callback) {
    callback = callback || function () {};
    var payload = '';
    request.on('data', function(chunk) {
        payload += chunk.toString();
    });
    request.on('end', function() {
        callback(payload);
    });
    response.writeHead(200);
    response.end();
}

function startHttpsServer(port, callback, done) {

    var options = {
        key: fs.readFileSync(integrationTestPath + 'localhost.key'),
        cert: fs.readFileSync(integrationTestPath + 'localhost.cert')
    };

    var server = https.createServer(options, function (request, response) {
        serverResponse(request, response, callback);
    });

    server.on('connection', function(socket) {
        socket.setTimeout(1000);
    });

    server.listen(port, function () {
        done();
    });

    return server;
}

function closeServer(callback, description) {
    description = description || 'none';
    callback = callback || function () {};
    var closed = false;
    runs(function () {
        console.log('closing server for ', description)
        server.close(function () {
            closed = true;
        });

        callback();
    });

    waitsFor(function() {
        return closed;
    }, 13000);
}

function parseJson(response, description) {
    try {
        return JSON.parse(response.body);
    } catch (e) {
        console.log("unable to parse json", response.statusCode, response.req.path, response.req.method, description, e);
        return {};
    }
}

exports.getLocation = function getResponse(url, status, expectedLocation, done) {
    request.get({url: url, followRedirect: false},
        function (err, response, body) {
            expect(err).toBeNull();
            expect(response.statusCode).toBe(status);
            if (expectedLocation) {
                expect(response.headers.location).toBe(expectedLocation);
            }
            done();
        });
}

exports.getQuery = function getResponse(url, status, expectedUris, done) {
    request.get({url: url, followRedirect: true},
        function (err, response, body) {
            expect(err).toBeNull();
            expect(response.statusCode).toBe(status);
            if (expectedUris) {
                var parsed = parseJson(response);
                expect(parsed._links.uris.length).toBe(expectedUris.length);
                for (var i = 0; i < expectedUris.length; i++) {
                    expect(parsed._links.uris[i]).toBe(expectedUris[i]);
                }
            }
            done();
        });
}

exports.runInTestChannel = runInTestChannel;
exports.download = download;
exports.randomChannelName = randomChannelName;
exports.configureFrisby = configureFrisby;
exports.runInTestChannelJson = runInTestChannelJson;
exports.createChannel = createChannel;
exports.getChannel = getChannel;
exports.addItem = addItem;
exports.timeout = timeout;
exports.getWebhookUrl = getGroupUrl;
exports.putWebhook = putGroup;
exports.getWebhook = getGroup;
exports.deleteWebhook = deleteGroup;
exports.postItem = postItem;
exports.postItemQ = postItemQ;
exports.startServer = startServer;
exports.startHttpsServer = startHttpsServer;
exports.closeServer = closeServer;
exports.getQ = getQ;
exports.getPort = getPort;
exports.parseJson = parseJson;
exports.itRefreshesChannels = itRefreshesChannels;

