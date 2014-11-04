require('./../integration/integration_config.js');

var request = require('request');
var http = require('http');
var moment = require('moment');
var channelName = utils.randomChannelName();
var groupName = utils.randomChannelName();
var channelResource = channelUrl + "/" + channelName;
var testName = __filename;


/**
 * This should:
 *
 * verify redirects for second, minute, hour
 */
describe(testName, function () {
    console.log('channelName ' + channelName);
    utils.createChannel(channelName);

    var timeUrl;
    it('gets time url', function (done) {
        request.get({url : channelResource, json : true },
            function (err, response, body) {
                expect(err).toBeNull();
                expect(response.statusCode).toBe(200);
                timeUrl = body._links.time.href;
                done();
            })

    });

    it('gets default redirect', function (done) {
        verifyRedirect(timeUrl, '/YYYY/MM/DD/HH/mm/ss/', done);
    });

    it('gets seconds redirect', function (done) {
        verifyRedirect(timeUrl + '/second', '/YYYY/MM/DD/HH/mm/ss/', done);
    });

    it('gets minutes redirect', function (done) {
        verifyRedirect(timeUrl + '/minute', '/YYYY/MM/DD/HH/mm/', done);
    });

    it('gets hour redirect', function (done) {
        verifyRedirect(timeUrl + '/hour', '/YYYY/MM/DD/HH/', done);
    });


    function verifyRedirect(url, format, done) {
        var time = moment().utc().format(format);
        request.get({url : url, followRedirect : false },
            function (err, response, body) {
                expect(err).toBeNull();
                expect(response.statusCode).toBe(303);
                expect(response.headers.location).toBe(channelResource + time);
                done();
            })
    }


});

