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

    it('gets tag hour ' + tag, function (done) {
        request.get({
                url: hubUrlBase + '/tag/' + tag + '/time/hour?stable=false',
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
        console.log('getting \'' + url + '\'');
        request.get({
                url: url
            },
            function (err, response, body) {
                expect(err).toBeNull();
                expect(response.statusCode).toBe(200);
                body = JSON.parse(body);
                parsedLinks = parse(response.headers.link);
                console.log('parsed', parsedLinks);
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


});

