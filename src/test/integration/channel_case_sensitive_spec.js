require('./integration_config.js');

var request = require('request');
var moment = require('moment');
var channelName = utils.randomChannelName() + '_AbCdE';
var channelResource = channelUrl + "/" + channelName;
var testName = __filename;

describe(testName, function () {

    var startTime = moment.utc().subtract(1, 'minute');

    console.log('channel url', channelResource);
    it("creates channel " + channelName + " at " + channelUrl, function (done) {
        console.log('startTime', startTime.format('/YYYY/MM/DD/HH/mm/ss/SSS'));
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
        utils.postItemQ(upperCase + '?forceWrite=true')
            .then(function (value) {
                posted = value.response.headers.location;
                console.log('posted', posted);
                done();
            });
    });


    it("verifies get upper case " + upperCase, function (done) {
        getUrl(posted, done);
    });

    utils.addItem(lowerCase + '?forceWrite=true', 201);

    var uris;

    function getTwo(channelUrl, path, done) {
        var url = channelUrl + path;
        console.log('calling', url);
        request.get({url: url},
            function (err, response, body) {
                expect(err).toBeNull();
                expect(response.statusCode).toBe(200);
                var parsed = utils.parseJson(response, 'time hour');
                uris = parsed._links.uris;
                if (uris.length !== 2) {
                    //console.log('parsed', parsed);
                }
                expect(uris.length).toBe(2);

                if (uris.length >= 2) {
                    expect(uris[1]).toContain(channelUrl);
                }
                if (uris.length >= 1) {
                    expect(uris[0]).toContain(channelUrl);
                    getUrl(uris[0], done);
                } else {
                    done();
                }
            });
    }

    /*it("gets time hour " + lowerCase, function (done) {
        getTwo(lowerCase, '/time/hour?stable=false', done);
    });

    it("gets time hour " + upperCase, function (done) {
        getTwo(upperCase, '/time/hour?stable=false', done);
     });*/

    // this delay is to allow the item time for the S3 write.
    utils.itSleeps(5000);

    it("gets latest 2 " + upperCase, function (done) {
        getTwo(upperCase, '/latest/2?stable=false', done);
    });

    /*it("gets time hour LONG_TERM_SINGLE " + lowerCase, function (done) {
        getTwo(lowerCase, '/time/hour?location=LONG_TERM_SINGLE&stable=false&trace=true', done);
    });

    it("gets first url remote ", function (done) {
        getUrl(uris[0] + '?remoteOnly=true', done);
    });

    it("gets second url remote ", function (done) {
        getUrl(uris[1] + '?remoteOnly=true', done);
     });*/

    it("gets next 2 " + lowerCase, function (done) {
        getTwo(lowerCase, startTime.format('/YYYY/MM/DD/HH/mm/ss/SSS') + '/A/next/2?stable=false&trace=true', done);
    });

    it("gets next 2 " + upperCase, function (done) {
        getTwo(upperCase, startTime.format('/YYYY/MM/DD/HH/mm/ss/SSS') + '/A/next/2?stable=false&trace=true', done);
    });

    /*it("gets prev 2 " + lowerCase, function (done) {
        getTwo(lowerCase, moment.utc().format('/YYYY/MM/DD/HH/mm/ss/SSS') + '/A/prev/2?stable=false', done);
    });

    it("gets prev 2 " + upperCase, function (done) {
        getTwo(upperCase, moment.utc().format('/YYYY/MM/DD/HH/mm/ss/SSS') + '/A/prev/2?stable=false', done);
    });

    it("gets earliest 2 " + lowerCase, function (done) {
        getTwo(lowerCase, '/earliest/2?stable=false', done);
    });

    it("gets earliest 2 " + upperCase, function (done) {
        getTwo(upperCase, '/earliest/2?stable=false', done);
     });*/



});

