require('./integration_config.js');

var request = require('request');
var channelName = utils.randomChannelName();
var channelResource = channelUrl + "/" + channelName;
var testName = __filename;

/**
 * verify that X-Forwarded-Host and X-Forwarded-Proto are respected.
 *
 * todo - gfm - this test has a practical flaw in how it works with ALB.
 * Disabling for now
 */
xdescribe(testName, function () {

    it("gets root url ", function (done) {
        console.log("hubUrlBase" + hubUrlBase);
        request.get({
                url: hubUrlBase,
                followRedirect: true,
                json: true,
                headers: {
                    'X-Forwarded-Host': 'headers',
                    'X-Forwarded-Proto': 'https'
                }
            },
            function (err, response, body) {
                expect(err).toBeNull();
                expect(response.statusCode).toBe(200);
                console.log(body);
                expect(body._links.self.href).toBe("https://headers/");
                done();
            });
    });

    it("gets channel url ", function (done) {
        request.get({
                url: hubUrlBase + "/channel",
                followRedirect: true,
                json: true,
                headers: {
                    'X-Forwarded-Host': 'headers',
                    'X-Forwarded-Proto': 'https'
                }
            },
            function (err, response, body) {
                expect(err).toBeNull();
                expect(response.statusCode).toBe(200);
                console.log(body);
                expect(body._links.self.href).toBe("https://headers/channel");
                done();
            });
    });

});
