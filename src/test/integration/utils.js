require('./integration_config.js');
var frisby = require('frisby');
var http = require('http');
var https = require('https');
var fs = require('fs');
var request = require('request');
var Q = require('q');

exports.runInTestChannel = function runInTestChannel(testName, channelName, functionToExecute) {
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

exports.randomChannelName = function randomChannelName() {
    return "test_" + Math.random().toString().replace(".", "_");
}

exports.download = function download(url, completionHandler) {
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

exports.configureFrisby = function configureFrisby(timeout) {
    timeout = typeof timeout !== 'undefined' ? timeout : 30000;
    frisby.globalSetup({
        timeout: timeout
    });
}

exports.createChannel = function createChannel(channelName, url) {
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
    }, 10 * 1001);

}

exports.putChannel = function putChannel(channelName, verify, body) {
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

exports.addItem = function addItem(url, responseCode) {
    it("adds item to " + url, function (done) {
        postItem(url, responseCode, done);
    }, 5099);
}

exports.postItem = function postItem(url, responseCode, completed) {
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

exports.postItemQ = function postItemQ(url) {
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

exports.putGroup = function putGroup(groupName, groupConfig, status) {
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
                    expect(parse.name).toBe(groupName);
                }
                done();
            });
    });
    return groupResource;
}

exports.getGroup = function getGroup(groupName, groupConfig, status) {
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

exports.deleteGroup = function deleteGroup(groupName) {
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

exports.getQ = function getQ(url, status, stable) {
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

exports.sleep = function sleep(millis) {
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

exports.timeout = function timeout(millis) {
    it('waits for ' + millis, function (done) {
        setTimeout(function () {
            done()
        }, millis);
    });
}

exports.startServer = function startServer(port, callback) {
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

exports.serverResponse = function serverResponse(request, response, callback) {
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

exports.startHttpsServer = function startHttpsServer(port, callback, done) {

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

exports.closeServer = function closeServer(callback) {
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

