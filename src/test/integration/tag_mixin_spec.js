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

    it('gets last link ', function (done) {
        request.get({
                url: uris[2]
            },
            function (err, response, body) {
                expect(err).toBeNull();
                expect(response.statusCode).toBe(200);
                body = JSON.parse(body);
                parsedLinks = parse(response.headers.link);
                console.log('first parsed', parsedLinks);
                expect(parsedLinks.previous.url).toContain('/previous?tag=' + tag)
                expect(parsedLinks.next.url).toContain('/next?tag=' + tag)

                done();
            });
    })

    /*    it('gets previous link ', function (done) {
     //this should redirect to the tag interface, the redirect to uri[1]
     console.log('getting', parsedLinks.previous.url);
     request.get({
     url: parsedLinks.previous.url
     },
     function (err, response, body) {
     expect(err).toBeNull();
     expect(response.statusCode).toBe(200);
     body = JSON.parse(body);
     parsedLinks = parse(response.headers.link);
     console.log('second parsed', parsedLinks);
     console.log(response);
     expect(parsedLinks.previous.url).toContain('/previous?tag=' + tag)
     expect(parsedLinks.previous.url).toContain(channelB)
     expect(parsedLinks.next.url).toContain('/next?tag=' + tag)

     done();
     });
     })*/



});

