require('../integration_config');

var request = require('request');
var moment = require('moment');
var channelName = utils.randomChannelName();
var channelResource = channelUrl + "/" + channelName;
var testName = __filename;

/**
 * This should:
 *
 * 1 - create a channel
 * 2 - get time links for that channel
 * 3 - verify that verify templates are correct
 */
describe(testName, function () {
    utils.createChannel(channelName);

    function verifyLinks(body, url, time, params) {
        params = params || '';
        if (body._links) {
            expect(body._links.self.href).toBe(url + params);
            expect(body._links.second.template).toBe(url + '/{year}/{month}/{day}/{hour}/{minute}/{second}{?stable}');
            expect(body._links.second.redirect).toBe(url + '/second' + params);

            expect(body._links.minute.href).toBe(channelResource + time.format('/YYYY/MM/DD/HH/mm') + params);
            expect(body._links.minute.template).toBe(url + '/{year}/{month}/{day}/{hour}/{minute}{?stable}');
            expect(body._links.minute.redirect).toBe(url + '/minute' + params);

            expect(body._links.hour.href).toBe(channelResource + time.format('/YYYY/MM/DD/HH') + params);
            expect(body._links.hour.template).toBe(url + '/{year}/{month}/{day}/{hour}{?stable}');
            expect(body._links.hour.redirect).toBe(url + '/hour' + params);

            expect(body._links.day.href).toBe(channelResource + time.format('/YYYY/MM/DD') + params);
            expect(body._links.day.template).toBe(url + '/{year}/{month}/{day}{?stable}');
            expect(body._links.day.redirect).toBe(url + '/day' + params);
        } else {
            expect(body._links).toBe(true);
        }
    }

    it('gets stable time links', function (done) {
        var url = channelResource + '/time';
        request.get({url : url, json : true},
            function (err, response, body) {
                expect(err).toBeNull();
                expect(response.statusCode).toBe(200);
                verifyLinks(body, url, moment(body.stable.millis).utc());
                done();
            })
    });

    it('gets stable param time links', function (done) {
        var url = channelResource + '/time';
        var params = '?stable=true';
        request.get({url : url + params, json : true},
            function (err, response, body) {
                expect(err).toBeNull();
                expect(response.statusCode).toBe(200);
                verifyLinks(body, url, moment(body.stable.millis).utc(), params);
                done();
            })
    });

    it('gets unstable time links', function (done) {
        var url = channelResource + '/time';
        var params = '?stable=false';
        request.get({url : url + params, json : true},
            function (err, response, body) {
                var time = moment().utc();
                expect(err).toBeNull();
                expect(response.statusCode).toBe(200);
                verifyLinks(body, url, moment(body.now.millis).utc(), params);
                done();
            })
    });


});

