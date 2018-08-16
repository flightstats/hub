require('../integration_config');
var http = require('http');
var https = require('https');
var fs = require('fs');
var request = require('request');
const moment = require('moment');

/**
 * Monkey patching Promise.prototype.finally until its officially supported
 *  proposal: https://github.com/tc39/proposal-promise-finally
 */

Promise.prototype.finally = function (callback) {
    const res = () => this;
    const fin = () => Promise.resolve(callback()).then(res);
    return this.then(fin, fin).catch(() => {});
};

exports.randomChannelName = function randomChannelName() {
    return "TeSt_" + Math.random().toString().replace(".", "_");
};

exports.randomNumberBetweenInclusive = function randomNumberBetweenInclusive(min, max) {
    min = Math.ceil(min);
    max = Math.floor(max);
    return Math.floor(Math.random() * (max - min + 1)) + min;
};

exports.randomTag = function randomTagName() {
    return "tag" + Math.random().toString().replace(".", "");
};

exports.getItem = function getItem(uri, callback) {
    request({uri: uri, encoding: null}, function (error, response, body) {
        expect(error).toBe(null);
        if (error) {
            console.log('got error ', uri, error);
        } else {
            if (response.statusCode !== 200) {
                console.log('wrong status ', uri, response.statusCode);
            }
            expect(response.statusCode).toBe(200);
        }
        callback(response.headers, body);
    });
};

exports.createChannel = function createChannel(channelName, url, description) {
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

};

exports.createChannelWithConfig = function createChannelWithConfig(channelName, config, url) {
    url = url || channelUrl + "/" + channelName;
    it("creates channel " + channelName + " at " + url, function (done) {
        console.log('creating channel ' + config.name);
        request.put({
                url: url,
                headers: {"Content-Type": "application/json"},
                body: JSON.stringify(config)
            },
            function (err, response, body) {
                expect(err).toBeNull();
                expect(response.statusCode).toBe(201);
                done();
            });
    }, 10 * 1001);

};

exports.httpGet = function httpGet(url, headers, isBinary) {
    if (headers)
        headers = utils.keysToLowerCase(headers);

    var options = {
        url: url,
        headers: headers || {},
        followRedirect: false
    };

    if (utils.isReceivingJSON(options.headers)) {
        options.json = true;
    }

    if (isBinary)
        options.encoding = null;

    return new Promise((resolve, reject) => {
        console.log('GET >', options.url, options.headers);
        request.get(options, function (error, response) {
            if (error)
                reject(error);
            else {
                console.log('GET <', options.url, response.statusCode);
                if (!options.json && utils.isSendingJSON(response.headers)) {
                    try {
                        response.body = JSON.parse(response.body);
                    } catch (error) {
                        console.warn('Response header says the content is JSON but it couldn\'t be parsed');
                    }
                }

                resolve(response);
            }
        });
    });
};

exports.httpPost = function httpPost(url, headers, body) {
    if (headers)
        headers = utils.keysToLowerCase(headers);

    var options = {
        url: url,
        headers: headers || {},
        body: body || ''
    };

    if (utils.isSendingOrReceivingJSON(headers)) {
        options.json = true;
    }

    let bytes = (options.json) ? JSON.stringify(options.body).length : options.body.length;

    return new Promise((resolve, reject) => {
        console.log('POST >', options.url, options.headers, bytes);
        request.post(options, function (error, response) {
            if (error)
                reject(error);
            else {
                console.log('POST <', options.url, response.statusCode);
                if (!options.json && utils.isSendingJSON(response.headers)) {
                    try {
                        response.body = JSON.parse(response.body);
                    } catch (error) {
                        console.warn('Response header says the content is JSON but it couldn\'t be parsed');
                    }
                }

                resolve(response);
            }
        });
    });
};

exports.httpPut = function httpPut(url, headers, body) {
    if (headers)
        headers = utils.keysToLowerCase(headers);

    var options = {
        url: url,
        headers: headers || {},
        body: body || ''
    };

    if (utils.isSendingOrReceivingJSON(headers)) {
        options.json = true;
    }

    let bytes = (options.json) ? JSON.stringify(options.body).length : options.body.length;

    return new Promise((resolve, reject) => {
        console.log('PUT >', options.url, options.headers, bytes);
        request.put(options, function (error, response) {
            if (error)
                reject(error);
            else {
                console.log('PUT <', options.url, response.statusCode);
                resolve(response);
            }
        });
    });
};

exports.httpDelete = function httpDelete(url, headers) {
    if (headers)
        headers = utils.keysToLowerCase(headers);

    var options = {
        url: url,
        headers: headers || {}
    };

    return new Promise((resolve, reject) => {
        console.log('DELETE >', options.url, options.headers);
        request.del(options, function (error, response) {
            if (error)
                reject(error);
            else {
                console.log('DELETE <', options.url, response.statusCode);
                resolve(response);
            }
        });
    });
};

exports.httpPatch = function httpPatch(url, headers, body) {
    if (headers)
        headers = utils.keysToLowerCase(headers);

    var options = {
        url: url,
        headers: headers || {},
        body: body || ''
    };

    if (utils.isSendingJSON(headers)) {
        options.json = true;
    }

    return new Promise((resolve, reject) => {
        console.log('PATCH', options.url, options.headers, options.body.length);
        request.patch(options, function (error, response) {
            if (error)
                reject(error);
            else
                resolve(response);
        });
    });
};

exports.isSendingJSON = function contentIsJSON(headers) {
    if (headers === undefined) return false;
    var hasContentType = 'content-type' in headers;
    var contentTypeIsJSON = headers['content-type'] === 'application/json';
    return hasContentType && contentTypeIsJSON;
};

exports.isReceivingJSON = function responseShouldBeJSON(headers) {
    if (headers === undefined) return false;
    let hasAcceptHeader = 'accept' in headers;
    let acceptIsJSON = headers['accept'] == 'application/json';
    return hasAcceptHeader && acceptIsJSON;
};

exports.isSendingOrReceivingJSON = function isSendingOrReceivingJSON(headers) {
    return utils.isSendingJSON(headers) || utils.isReceivingJSON(headers);
};

exports.keysToLowerCase = function keysToLowerCase(obj) {
    var output = {};
    var keys = Object.keys(obj);
    for (var i = 0; i < keys.length; ++i) {
        var originalKey = keys[i];
        var lowerCaseKey = utils.toLowerCase(originalKey);
        output[lowerCaseKey] = obj[originalKey];
    }
    return output;
};

exports.toLowerCase = function toLowerCase(str) {
    var output = '';
    for (var i = 0; i < str.length; ++i) {
        var character = str[i];
        var code = parseInt(character, 36) || character;
        output += code.toString(36);
    }
    return output;
};

exports.putChannel = function putChannel (channelName, verify, body, description, expectedStatus) {
    expectedStatus = expectedStatus || 201;
    verify = verify || function () {};
    body = body || { name: channelName };
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
            console.log("respinse " + body);
            verify(response, body);
            done();
        });
    });
};

exports.getChannel = function getChannel(channelName, verify, description, hubUrl) {
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
                console.log("get response " + body);
                verify(response, body, hubUrl);
                done();
            });
    });
};

exports.addItem = function addItem(url, responseCode) {
    it("adds item to " + url, function (done) {
        utils.postItem(url, responseCode, done);
    }, 5099);
};

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
};

function postItemQwithPayload(url, headers, body) {
    var options = {
        url: url,
        headers: headers || {},
        body: body
    };

    if ('Content-Type' in options.headers && options.headers['Content-Type'] === 'application/json') {
        options = Object.assign({}, options, {json: true});
    }

    return new Promise((resolve, reject) => {
        request.post(options, function (error, response, body) {
            expect(error).toBeNull();
            expect(response.statusCode).toBe(201);
            resolve({response: response, body: body});
        });
    });
}

exports.postItemQwithPayload = postItemQwithPayload;

exports.postItemQ = function postItemQ(url) {
    //with default json payload
    //todo - gfm - this is effectively rolled back to the function from ~6/16
    var payload = {
        url: url, json: true,
        headers: {"Content-Type": "application/json"},
        body: JSON.stringify({"data": Date.now()})
    };
    return new Promise((resolve, reject) => {
        request.post(payload,
            function (err, response, body) {
                expect(err).toBeNull();
                expect(response.statusCode).toBe(201);
                resolve({response: response, body: body});
            });
    });
};

exports.getWebhookUrl = function getWebhookUrl() {
    if (Math.random() > 0.5) {
        return hubUrlBase + '/webhook';
    }
    return hubUrlBase + '/group';
};

exports.putWebhook = function putGroup(groupName, groupConfig, status, description, groupUrl) {
    description = description || 'none';
    status = status || 201;
    groupUrl = groupUrl || utils.getWebhookUrl();
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
};

exports.getWebhook = function getGroup(groupName, groupConfig, status, verify) {
    var groupResource = utils.getWebhookUrl() + "/" + groupName;
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
};

exports.deleteWebhook = function deleteGroup(groupName) {
    var groupResource = utils.getWebhookUrl() + "/" + groupName;
    it('deletes the group ' + groupName, function (done) {
        request.del({url: groupResource },
            function (err, response, body) {
                expect(err).toBeNull();
                expect(response.statusCode).toBe(202);
                done();
            });
    }, 60 * 1000);
};

exports.itRefreshesChannels = function itRefreshesChannels() {
    it('refreshes channels', function (done) {
        request.get(hubUrlBase + '/internal/channel/refresh',
            function (err, response, body) {
                expect(err).toBeNull();
                expect(response.statusCode).toBe(200);
                console.log('refresh', body);
                done();
            });
    });
};

exports.getQ = function getQ(url, status, stable) {
    status = status || 200;
    stable = stable || false;
    return new Promise((resolve, reject) => {
        request.get({url: url + '?stable=' + stable, json: true},
            function (err, response, body) {
                expect(err).toBeNull();
                expect(response.statusCode).toBe(status);
                resolve({response: response, body: body});
            });
    });
};

exports.itSleeps = function itSleeps(millis) {
    it('sleeps for ' + millis + 'ms', function (done) {
        setTimeout(done, millis);
    });
};

exports.itMessages = function itSleeps(msg) {
    it(msg || "", function (done) {
        done();
    });
};

exports.getPort = function getPort() {
    var port = callbackPort++;
    console.log('using port', port);
    return port;
};

exports.startHttpServer = function startHttpServer(port, callback, done) {
    var httpServer = new http.Server();
    return utils.startServer(httpServer, port, callback, done);
};

exports.startHttpsServer = function startHttpsServer(port, callback, done) {
    var options = {
        key: fs.readFileSync('localhost.key'),
        cert: fs.readFileSync('localhost.cert')
    };
    var httpsServer = new https.Server(options);
    return utils.startServer(httpsServer, port, callback, done);
};

exports.startServer = function startServer(server, port, callback, done) {
    server.on('connection', function (socket) {
        socket.setTimeout(1000);
    });

    server.on('request', function (request, response) {
        var incoming = '';

        request.on('data', function (chunk) {
            incoming += chunk.toString();
        });

        request.on('end', function () {
            if (callback) callback(incoming, response);
            response.end();
        });
    });

    server.on('listening', function () {
        console.log(`server listening at ${getCallBackDomain()}:${port}/`);
        done();
    });

    server.listen(port);

    return server;
};

exports.closeServer = function closeServer(server, callback) {
    console.log('closing server on port', server.address().port);
    callback = callback || function () {};
    server.close(callback);
};

exports.parseJson = function parseJson(response, description) {
    try {
        return JSON.parse(response.body);
    } catch (e) {
        console.log("unable to parse json", response.statusCode, response.req.path, response.req.method, description, e);
        return {};
    }
};

exports.getLocation = function getLocation(url, status, expectedLocation, done) {
    request.get({url: url, followRedirect: false},
        function (err, response, body) {
            expect(err).toBeNull();
            expect(response.statusCode).toBe(status);
            if (expectedLocation) {
                expect(response.headers.location).toBe(expectedLocation);
            }
            done();
        });
};

exports.getQuery = function getQuery(url, status, expectedUris, done) {
    request.get({url: url, followRedirect: true},
        function (err, response, body) {
            expect(err).toBeNull();
            expect(response.statusCode).toBe(status);
            if (expectedUris) {
                var parsed = utils.parseJson(response);
                expect(parsed._links.uris.length).toBe(expectedUris.length);
                for (var i = 0; i < expectedUris.length; i++) {
                    expect(parsed._links.uris[i]).toBe(expectedUris[i]);
                }
            }
            done();
        });
};

exports.waitForData = function waitForData(actual, expected, done) {
    expect(actual).isPrototypeOf(Array);
    expect(expected).isPrototypeOf(Array);
    setTimeout(function () {
        if (actual.length !== expected.length) {
            waitForData(actual, expected, done);
        } else {
            console.log('expected:', expected);
            console.log('actual:', actual);
            done();
        }
    }, 500);
};

exports.arrayEquals = function arrayEquals(a, b) {
    if (a === b) return true;
    if (a == null || b == null) return false;
    if (a.length != b.length) return false;
    for (let i = 0; i < a.length; ++i) {
        if (a[i] !== b[i]) return false;
    }
    return true;
};

exports.isRedirect = function isRedirect(response) {
    return response.statusCode >= 300 && response.statusCode <= 399;
};

exports.followRedirectIfPresent = function followRedirectIfPresent(response) {
    if (utils.isRedirect(response)) {
        return utils.httpGet(response.headers.location);
    } else {
        return response;
    }
};

/**
 * This function will query the url every second and execute the clause callback
 * until either the timeout is reached or the clause callback returns true.
 *
 * @param {string} url HTTP endpoint to query
 * @param {function} clause function that is run against each response
 * @param {number} [timeoutMS=30000] when to give up
 * @param {number} [delayMS=1000] how long to wait between attempts
 * @returns {Promise}
 */
exports.httpGetUntil = function httpGetUntil(url, clause, timeoutMS, delayMS) {
    let started = moment().utc();
    let timeout = moment().utc().add(timeoutMS || 30000, 'ms');

    return new Promise((resolve, reject) => {
        function loop() {
            let now = moment.utc();
            if (now.isSameOrAfter(timeout)) {
                reject('timed out: ' + now.diff(started) + 'ms');
            }

            utils.httpGet(url)
                .then(response => {
                    if (clause(response)) {
                        resolve(response);
                    } else {
                        setTimeout(loop, delayMS || 1000);
                    }
                })
                .catch(error => reject(error));
        }

        loop();
    });
};
