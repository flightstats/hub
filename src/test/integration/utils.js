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
    return "test_" + Math.random().toString().replace(".", "-");
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

function putChannel(channelName, verify, body) {
    verify = verify || function () {};
    body = body || {"name" : channelName};
    it("puts channel " + channelName + " at " + channelUrl, function (done) {
        request.put({url: channelUrl + '/' + channelName,
                headers: {"Content-Type": "application/json"},
                body: JSON.stringify(body)},
            function (err, response, body) {
                expect(err).toBeNull();
                expect(response.statusCode).toBe(201);
                verify(response, body);
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

function putGroup(groupName, groupConfig, status, description) {
    description = description || 'none';
    status = status || 201;
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

function getGroup(groupName, groupConfig, status) {
    var groupResource = groupUrl + "/" + groupName;
    status = status || 200;

    it('gets group ' + groupName, function (done) {
        sleep(10000);
        request.get({url : groupResource,
                headers : {"Content-Type" : "application/json"} },
            function (err, response, body) {
                expect(err).toBeNull();
                expect(response.statusCode).toBe(status);
                if (response.statusCode < 400) {
                    var parse = utils.parseJson(response, groupName);
                    expect(parse._links.self.href).toBe(groupResource);
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
    }, 60 * 1000);
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

function timeout(millis) {
    it('waits for ' + millis, function (done) {
        setTimeout(function () {
            done()
        }, millis);
    });
}

function getPort() {
    return callbackPort++;
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

exports.runInTestChannel = runInTestChannel;
exports.download = download;
exports.randomChannelName = randomChannelName;
exports.configureFrisby = configureFrisby;
exports.runInTestChannelJson = runInTestChannelJson;
exports.createChannel = createChannel;
exports.putChannel = putChannel;
exports.addItem = addItem;
exports.sleep = sleep;
exports.timeout = timeout;
exports.putGroup = putGroup;
exports.getGroup = getGroup;
exports.deleteGroup = deleteGroup;
exports.postItem = postItem;
exports.postItemQ = postItemQ;
exports.startServer = startServer;
exports.startHttpsServer = startHttpsServer;
exports.closeServer = closeServer;
exports.getQ = getQ;
exports.getPort = getPort;
exports.parseJson = parseJson;

