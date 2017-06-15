require('./integration_config.js');

var request = require('request');
var channelName = utils.randomChannelName() + '_AbCdE';
var channelResource = channelUrl + "/" + channelName;
var testName = __filename;

describe(testName, function () {
    console.log('channel url', channelResource);
    it("creates channel " + channelName + " at " + channelUrl, function (done) {
        request.put({
                url: channelResource,
                headers: {"Content-Type": "application/json"},
                body: JSON.stringify({"ttlDays": 1})
            },
            function (err, response, body) {
                expect(err).toBeNull();
                expect(response.statusCode).toBe(201);
                done();
            });
    });

    var lowerCase = channelUrl + "/" + channelName.toLowerCase();

    function getUrl(url, done) {
        done = done || function () {
            };
        request.get({url: url},
            function (err, response, body) {
                expect(err).toBeNull();
                expect(response.statusCode).toBe(200);
                if (body.indexOf('{"data":') === -1) {
                    var substring = url;
                    if (url.indexOf("?") > 0) {
                        substring = url.substring(0, url.indexOf("?"));
                    }
                    expect(body).toContain(substring);
                }
                done();
            });
    }

    it("verifies channel lower case " + lowerCase, function (done) {
        getUrl(lowerCase, done);
    });

    it("verifies channel lower case " + lowerCase, function (done) {
        getUrl(lowerCase + '?cached=false', done);
    });

    var upperCase = channelUrl + "/" + channelName.toUpperCase();

    it("verifies channel upper case " + upperCase, function (done) {
        getUrl(upperCase, done);
    });

    it("verifies channel upper case " + upperCase, function (done) {
        getUrl(upperCase + '?cached=false', done);
    });

    var posted;

    it('posts item', function (done) {
        utils.postItemQ(upperCase)
            .then(function (value) {
                posted = value.response.headers.location;
                console.log('posted', posted);
                done();
            });
    });


    it("verifies get upper case " + upperCase, function (done) {
        getUrl(posted, done);
    });

    utils.addItem(lowerCase, 201);

    var uris;

    function getTwo(channelUrl, path, done) {

        request.get({url: channelUrl + path},
            function (err, response, body) {
                expect(err).toBeNull();
                expect(response.statusCode).toBe(200);
                var parsed = utils.parseJson(response, 'time hour');
                uris = parsed._links.uris;
                expect(uris.length).toBe(2);
                expect(uris[0]).toContain(channelUrl);
                expect(uris[1]).toContain(channelUrl);
                getUrl(uris[0], done);


            });
    }

    it("gets time hour " + lowerCase, function (done) {
        getTwo(lowerCase, '/time/hour?stable=false', done);
    });

    it("gets first url remote ", function (done) {
        utils.sleep(1000);
        getUrl(uris[0] + '?remoteOnly=true', done);
    });

    it("gets second url remote ", function (done) {
        getUrl(uris[1] + '?remoteOnly=true', done);
    });

    it("gets latest 2 " + upperCase, function (done) {
        getTwo(upperCase, '/latest/2?stable=false', done);
    });


    //todo - gfm - also use other API endpoints


    //todo - gfm - can we do a direct item GET from S3?


});

