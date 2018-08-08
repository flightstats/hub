require('../integration_config');
const {
    fromObjectPath,
    getProp,
} = require('../lib/helpers');
var request = require('request');
var parse = require('parse-link-header');
var channelA = utils.randomChannelName();
var channelB = utils.randomChannelName();
var tag = Math.random().toString().replace(".", "");
var testName = __filename;
var channelBody = {
    tags: [tag, "test"],
    ttlDays: 1
};

/**
 * This should:
 * Create ChannelA with tag TagA
 * Create ChannelB with tag TagA
 *
 * Add data to channel
 * Add data to channelB
 *
 * verify that tag time query can get data back out
 *
 */
describe(testName, function () {

    utils.putChannel(channelA, false, channelBody, testName);

    utils.putChannel(channelB, false, channelBody, testName);

    utils.itRefreshesChannels();

    utils.addItem(channelUrl + '/' + channelA, 201);

    utils.addItem(channelUrl + '/' + channelB, 201);

    utils.addItem(channelUrl + '/' + channelA, 201);

    var uris = [];

    var tagUrl = hubUrlBase + '/tag/' + tag;

    it('gets tag hour ' + tag, function (done) {
        var url = tagUrl + '/time/hour?stable=false&trace=true';
        console.log('calling tag hour ', url);
        request.get({
            url: url,
            headers: {"Content-Type": "application/json"}
        },
        function (err, response, body) {
            expect(err).toBeNull();
            const statusCode = getProp('statusCode', response);
            expect(statusCode).toBe(200);
            if (statusCode === 200) {
                body = utils.parseJson(response, testName);
                console.log('parsed tag body', body);
                const links = getProp('_links', body);
                uris = getProp('uris', links) || [];
                expect(uris.length).toBe(3);
                expect(uris[0]).toContain(channelA);
                expect(uris[1]).toContain(channelB);
                expect(uris[2]).toContain(channelA);
            } else {
                console.log('failing test, can\'t get uris . status=' + statusCode);
            }
            done();
        });
    }, 2 * 60001);

    var parsedLinks;

    function linkStripParams(uri) {
        return uri.substr(0, uri.indexOf('?'));
    }

    function traverse(url, index, done) {
        url = url.trim();
        console.log(' traverse ' + url + ' ' + testName);
        request.get({
            url: url
        },
        function (err, response, body) {
            expect(err).toBeNull();
            expect(getProp('statusCode', response)).toBe(200);
            if (getProp('statusCode', response) === 200) {
                body = utils.parseJson(response, testName);
                const links = fromObjectPath(['headers', 'link'], response) || '{}';
                parsedLinks = parse(links);
                var item = uris[index] && linkStripParams(uris[index]);
                if (parsedLinks) {
                    const prevUrl = fromObjectPath(['previous', 'url'], parsedLinks);
                    const nextUrl = fromObjectPath(['next', 'url'], parsedLinks);
                    expect(prevUrl).toContain(item + '/previous?tag=' + tag);
                    expect(nextUrl).toContain(item + '/next?tag=' + tag);
                } else {
                    expect(parsedLinks).toBe(true);
                }
            }
            done();
        });
    }

    it('gets last link ', function (done) {
        traverse(uris[2] + '&stable=false', 2, done);
    }, 60002);

    it('gets previous link ', function (done) {
        const prevUrl = fromObjectPath(['previous', 'url'], parsedLinks);
        traverse(prevUrl, 1, done);
    }, 60003);

    it('gets 2nd previous link ', function (done) {
        const prevUrl = fromObjectPath(['previous', 'url'], parsedLinks);
        traverse(prevUrl, 0, done);
    }, 60004);

    it('gets first link ', function (done) {
        traverse(uris[0], 0, done);
    }, 60005);

    it('gets next link ', function (done) {
        const nextUrl = fromObjectPath(['next', 'url'], parsedLinks);
        traverse(nextUrl + '&stable=false', 1, done);
    }, 60006);

    it('gets 2nd next link ', function (done) {
        const nextUrl = fromObjectPath(['next', 'url'], parsedLinks);
        traverse(nextUrl + '&stable=false', 2, done);
    }, 60007);

    it("gets latest unstable in tag ", function (done) {
        request.get({url: tagUrl + '/latest?stable=false', followRedirect: false},
            function (err, response, body) {
                expect(err).toBeNull();
                expect(getProp('statusCode', response)).toBe(303);
                const location = fromObjectPath(['headers', 'location'], response);
                expect(location).toBe(uris[2]);
                done();
            });
    }, 60008);

    it("gets latest N unstable in tag ", function (done) {
        request.get({url: tagUrl + '/latest/10?stable=false'},
            function (err, response, body) {
                expect(err).toBeNull();
                expect(getProp('statusCode', response)).toBe(200);
                var parsed = utils.parseJson(response, testName);
                const currentUris = fromObjectPath(['_links', 'uris'], parsed) || [];
                expect(currentUris.length).toBe(3);
                currentUris.forEach(function (uri, index) {
                    expect(uris[index]).toBe(uri);
                });

                done();
            });
    }, 60009);

    it("gets earliest unstable in channel " + tag, function (done) {
        request.get({url: tagUrl + '/earliest?stable=false&trace=true', followRedirect: false},
            function (err, response, body) {
                expect(err).toBeNull();
                expect(getProp('statusCode', response)).toBe(303);
                const location = fromObjectPath(['headers', 'location'], response);
                expect(location).toBe(uris[0]);
                done();
            });
    }, 60010);

    it("next from item " + tag, function (done) {
        var url = linkStripParams(uris[0]) + '/next/2?tag=' + tag + '&stable=false';
        request.get({url: url},
            function (err, response, body) {
                expect(err).toBeNull();
                expect(getProp('statusCode', response)).toBe(200);
                var parsed = utils.parseJson(response, testName);
                const currentUris = fromObjectPath(['_links', 'uris'], parsed) || [];
                expect(currentUris.length).toBe(2);
                currentUris.forEach(function (uri, index) {
                    console.log('found ', uri);
                    expect(uri).toBe(uris[index + 1]);
                });
                done();
            });
    }, 60011);

    it("previous from tag " + tag, function (done) {
        var last = linkStripParams(uris[2]).substring(uris[2].indexOf(channelA) + channelA.length);
        var url = tagUrl + last + '/previous/3?tag=' + tag + '&stable=false';
        request.get({url: url},
            function (err, response, body) {
                expect(err).toBeNull();
                expect(getProp('statusCode', response)).toBe(200);
                var parsed = utils.parseJson(response, testName);
                const currentUris = fromObjectPath(['_links', 'uris'], parsed) || [];
                expect(currentUris.length).toBe(2);
                currentUris.forEach(function (uri, index) {
                    console.log('found ', uri);
                    expect(uri).toBe(uris[index]);
                });

                done();
            });
    }, 60012);

    it("next from tag " + tag, function (done) {
        var last = linkStripParams(uris[0]).substring(uris[0].indexOf(channelA) + channelA.length);
        var url = tagUrl + last + '/next/3?tag=' + tag + '&stable=false';
        request.get({url: url},
            function (err, response, body) {
                expect(err).toBeNull();
                expect(getProp('statusCode', response)).toBe(200);
                var parsed = utils.parseJson(response, testName);
                const currentUris = fromObjectPath(['_links', 'uris'], parsed) || [];
                expect(currentUris.length).toBe(2);
                currentUris.forEach(function (uri, index) {
                    console.log('found ', uri);
                    expect(uri).toBe(uris[index + 1]);
                });
                done();
            });
    }, 60013);

    it("previous from item " + tag, function (done) {
        var url = linkStripParams(uris[2]) + '/previous/2?tag=' + tag + '&stable=false';
        request.get({url: url},
            function (err, response, body) {
                expect(err).toBeNull();
                expect(getProp('statusCode', response)).toBe(200);
                var parsed = utils.parseJson(response, testName);
                const currentUris = fromObjectPath(['_links', 'uris'], parsed) || [];
                expect(currentUris.length).toBe(2);
                currentUris.forEach(function (uri, index) {
                    console.log('found ', uri);
                    expect(uri).toBe(uris[index]);
                });

                done();
            });
    }, 60014);

    it("latest from channel " + tag, function (done) {
        var url = hubUrlBase + '/channel/' + channelB + '/latest?tag=' + tag + '&stable=false';
        request.get({url: url, followRedirect: false},
            function (err, response, body) {
                expect(err).toBeNull();
                const location = fromObjectPath(['headers', 'location'], response);
                expect(getProp('statusCode', response)).toBe(303);
                expect(location).toBe(uris[2]);
                done();
            });
    }, 60015);

    it("latest 3 from channel " + tag, function (done) {
        var url = hubUrlBase + '/channel/' + channelA + '/latest/3?tag=' + tag + '&stable=false';
        request.get({url: url, followRedirect: false},
            function (err, response, body) {
                expect(err).toBeNull();
                expect(getProp('statusCode', response)).toBe(200);
                var parsed = utils.parseJson(response, testName);
                const currentUris = fromObjectPath(['_links', 'uris'], parsed) || [];
                expect(currentUris.length).toBe(3);
                currentUris.forEach(function (uri, index) {
                    console.log('found ', uri);
                    expect(uri).toBe(uris[index]);
                });
                done();
            });
    }, 60016);

    it("earliest from channel " + tag, function (done) {
        var url = hubUrlBase + '/channel/' + channelB + '/earliest?tag=' + tag + '&stable=false&trace=true';
        request.get({url: url, followRedirect: false},
            function (err, response, body) {
                expect(err).toBeNull();
                expect(getProp('statusCode', response)).toBe(303);
                const location = fromObjectPath(['headers', 'location'], response);
                expect(location).toBe(uris[0]);
                done();
            });
    }, 60017);

    it("earliest 3 from channel " + tag, function (done) {
        var url = hubUrlBase + '/channel/' + channelA + '/earliest/3?tag=' + tag + '&stable=false&trace=true';
        request.get({url: url, followRedirect: false},
            function (err, response, body) {
                expect(err).toBeNull();
                expect(getProp('statusCode', response)).toBe(200);
                var parsed = utils.parseJson(response, testName);
                const currentUris = fromObjectPath(['_links', 'uris'], parsed) || [];
                expect(currentUris.length).toBe(3);
                currentUris.forEach(function (uri, index) {
                    console.log('found ', uri);
                    expect(uri).toBe(uris[index]);
                });
                done();
            });
    }, 60018);

    it("day query from channel " + tag, function (done) {
        var url = hubUrlBase + '/channel/' + channelB + '/time/day?tag=' + tag + '&stable=false';
        request.get({url: url},
            function (err, response, body) {
                expect(err).toBeNull();
                expect(getProp('statusCode', response)).toBe(200);
                var parsed = utils.parseJson(response, testName);
                console.log('parsed', parsed);
                const currentUris = fromObjectPath(['_links', 'uris'], parsed) || [];
                expect(currentUris.length).toBe(3);
                currentUris.forEach(function (uri, index) {
                    console.log('found ', uri);
                    expect(uri).toBe(uris[index]);
                });
                done();
            });
    }, 60019);

});
