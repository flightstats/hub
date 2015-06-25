require('./integration_config.js');

var request = require('request');
var http = require('http');
var parse = require('parse-link-header');
var channelA = utils.randomChannelName();
var channelB = utils.randomChannelName();
var tag = Math.random().toString().replace(".", "");
var testName = __filename;
var channelBody = {
    tags: [tag, "test"]
};

/**
 * This should:
 * Create ChannelA with tag TagA
 * Create ChannelB with tag TagA
 *
 * Add data to channelA
 * Add data to channelB
 *
 * verify that tag time query can get data back out
 *
 */
describe(testName, function () {

    utils.putChannel(channelA, false, channelBody);
    utils.putChannel(channelB, false, channelBody);

    utils.addItem(channelUrl + '/' + channelA, 201);

    utils.addItem(channelUrl + '/' + channelB, 201);

    utils.addItem(channelUrl + '/' + channelA, 201);

    var uris = [];

    var tagUrl = hubUrlBase + '/tag/' + tag;

    it('gets tag hour ' + tag, function (done) {
        request.get({
                url: tagUrl + '/time/hour?stable=false',
                headers: {"Content-Type": "application/json"}
            },
            function (err, response, body) {
                expect(err).toBeNull();
                expect(response.statusCode).toBe(200);
                body = JSON.parse(body);
                console.log(body);
                uris = body._links.uris;
                expect(uris.length).toBe(3);
                expect(uris[0]).toContain(channelA);
                expect(uris[1]).toContain(channelB);
                expect(uris[2]).toContain(channelA);
                done();
            });
    });

    var parsedLinks;

    function linkStripParams(uri) {
        return uri.substr(0, uri.indexOf('?'));
    }

    function traverse(url, index, done) {
        url = url.trim();
        request.get({
                url: url
            },
            function (err, response, body) {
                expect(err).toBeNull();
                expect(response.statusCode).toBe(200);
                body = JSON.parse(body);
                parsedLinks = parse(response.headers.link);
                var item = linkStripParams(uris[index]);
                expect(parsedLinks.previous.url).toContain(item + '/previous?tag=' + tag)
                expect(parsedLinks.next.url).toContain(item + '/next?tag=' + tag)

                done();
            });
    }

    it('gets last link ', function (done) {
        traverse(uris[2] + '&stable=false', 2, done);
    })

    it('gets previous link ', function (done) {
        traverse(parsedLinks.previous.url, 1, done);
    })

    it('gets 2nd previous link ', function (done) {
        traverse(parsedLinks.previous.url, 0, done);
    })

    it('gets first link ', function (done) {
        traverse(uris[0], 0, done);
    })

    it('gets next link ', function (done) {
        traverse(parsedLinks.next.url + '&stable=false', 1, done);
    })

    it('gets 2nd next link ', function (done) {
        traverse(parsedLinks.next.url + '&stable=false', 2, done);
    })

    it("gets latest unstable in tag ", function (done) {
        request.get({url: tagUrl + '/latest?stable=false', followRedirect: false},
            function (err, response, body) {
                expect(err).toBeNull();
                expect(response.statusCode).toBe(303);
                expect(response.headers.location).toBe(uris[2]);
                done();
            });
    });

    it("gets latest N unstable in tag ", function (done) {
        request.get({url: tagUrl + '/latest/10?stable=false'},
            function (err, response, body) {
                expect(err).toBeNull();
                expect(response.statusCode).toBe(200);
                var parsed = JSON.parse(response.body);
                expect(parsed._links.uris.length).toBe(3);
                uris.forEach(function (uri, index) {
                    expect(parsed._links.uris[index]).toBe(uri);
                });

                done();
            });
    });

    it("gets earliest unstable in channel " + tag, function (done) {
        request.get({url: tagUrl + '/earliest?stable=false&trace=true', followRedirect: false},
            function (err, response, body) {
                expect(err).toBeNull();
                expect(response.statusCode).toBe(303);
                expect(response.headers.location).toBe(uris[0]);
                done();
            });
    });

    it("next from item " + tag, function (done) {
        var url = linkStripParams(uris[0]) + '/next/2?tag=' + tag + '&stable=false';
        request.get({url: url},
            function (err, response, body) {
                expect(err).toBeNull();
                expect(response.statusCode).toBe(200);
                var parsed = JSON.parse(response.body);
                expect(parsed._links.uris.length).toBe(2);
                parsed._links.uris.forEach(function (uri, index) {
                    console.log('found ', uri);
                    expect(uri).toBe(uris[index + 1]);
                });
                done();
            });
    });

    it("previous from tag " + tag, function (done) {
        var last = linkStripParams(uris[2]).substring(uris[2].indexOf(channelA) + channelA.length);
        var url = tagUrl + last + '/previous/3?tag=' + tag + '&stable=false';
        request.get({url: url},
            function (err, response, body) {
                expect(err).toBeNull();
                expect(response.statusCode).toBe(200);
                var parsed = JSON.parse(response.body);
                expect(parsed._links.uris.length).toBe(2);
                parsed._links.uris.forEach(function (uri, index) {
                    console.log('found ', uri);
                    expect(uri).toBe(uris[index]);
                });
                done();
            });
    });

    it("next from tag " + tag, function (done) {
        var last = linkStripParams(uris[0]).substring(uris[0].indexOf(channelA) + channelA.length);
        var url = tagUrl + last + '/next/3?tag=' + tag + '&stable=false';
        request.get({url: url},
            function (err, response, body) {
                expect(err).toBeNull();
                expect(response.statusCode).toBe(200);
                var parsed = JSON.parse(response.body);
                expect(parsed._links.uris.length).toBe(2);
                parsed._links.uris.forEach(function (uri, index) {
                    console.log('found ', uri);
                    expect(uri).toBe(uris[index + 1]);
                });
                done();
            });
    });

    it("previous from item " + tag, function (done) {
        var url = linkStripParams(uris[2]) + '/previous/2?tag=' + tag + '&stable=false';
        request.get({url: url},
            function (err, response, body) {
                expect(err).toBeNull();
                expect(response.statusCode).toBe(200);
                var parsed = JSON.parse(response.body);
                expect(parsed._links.uris.length).toBe(2);
                parsed._links.uris.forEach(function (uri, index) {
                    console.log('found ', uri);
                    expect(uri).toBe(uris[index]);
                });
                done();
            });
    });

    it("latest from channel " + tag, function (done) {
        var url = hubUrlBase + '/channel/' + channelB + '/latest?tag=' + tag + '&stable=false';
        request.get({url: url, followRedirect: false},
            function (err, response, body) {
                expect(err).toBeNull();
                expect(response.statusCode).toBe(303);
                expect(response.headers.location).toBe(uris[2]);
                done();
            });
    });

    it("latest 3 from channel " + tag, function (done) {
        var url = hubUrlBase + '/channel/' + channelA + '/latest/3?tag=' + tag + '&stable=false';
        request.get({url: url, followRedirect: false},
            function (err, response, body) {
                expect(err).toBeNull();
                expect(response.statusCode).toBe(200);
                var parsed = JSON.parse(response.body);
                expect(parsed._links.uris.length).toBe(3);
                parsed._links.uris.forEach(function (uri, index) {
                    console.log('found ', uri);
                    expect(uri).toBe(uris[index]);
                });
                done();
            });
    });

    it("earliest from channel " + tag, function (done) {
        var url = hubUrlBase + '/channel/' + channelB + '/earliest?tag=' + tag + '&stable=false&trace=true';
        request.get({url: url, followRedirect: false},
            function (err, response, body) {
                expect(err).toBeNull();
                expect(response.statusCode).toBe(303);
                expect(response.headers.location).toBe(uris[0]);
                done();
            });
    });

    it("earliest 3 from channel " + tag, function (done) {
        var url = hubUrlBase + '/channel/' + channelA + '/earliest/3?tag=' + tag + '&stable=false&trace=true';
        request.get({url: url, followRedirect: false},
            function (err, response, body) {
                expect(err).toBeNull();
                expect(response.statusCode).toBe(200);
                var parsed = JSON.parse(response.body);
                expect(parsed._links.uris.length).toBe(3);
                parsed._links.uris.forEach(function (uri, index) {
                    console.log('found ', uri);
                    expect(uri).toBe(uris[index]);
                });
                done();
            });
    });

    it("day query from channel " + tag, function (done) {
        var url = hubUrlBase + '/channel/' + channelB + '/time/day?tag=' + tag + '&stable=false';
        request.get({url: url},
            function (err, response, body) {
                expect(err).toBeNull();
                expect(response.statusCode).toBe(200);
                var parsed = JSON.parse(response.body);
                console.log('parsed', parsed);
                expect(parsed._links.uris.length).toBe(3);
                parsed._links.uris.forEach(function (uri, index) {
                    console.log('found ', uri);
                    expect(uri).toBe(uris[index]);
                });
                done();
            });
    });


});

