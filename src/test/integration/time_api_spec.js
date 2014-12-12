require('./../integration/integration_config.js');

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

    it('get time links', function (done) {
        var url = channelResource + '/time';
        request.get({url : url, json : true},
            function (err, response, body) {

                var time = moment().utc().subtract(stableOffset, 'seconds');
                expect(err).toBeNull();
                expect(response.statusCode).toBe(200);
                expect(body._links.self.href).toBe(url);
                expect(body._links.second.href).toBe(channelResource + time.format('/YYYY/MM/DD/HH/mm/ss'));
                expect(body._links.second.template).toBe(url + '/{year}/{month}/{day}/{hour}/{minute}/{second}');
                expect(body._links.second.redirect).toBe(url + '/second');

                expect(body._links.minute.href).toBe(channelResource + time.format('/YYYY/MM/DD/HH/mm'));
                expect(body._links.minute.template).toBe(url + '/{year}/{month}/{day}/{hour}/{minute}');
                expect(body._links.minute.redirect).toBe(url + '/minute');

                expect(body._links.hour.href).toBe(channelResource + time.format('/YYYY/MM/DD/HH'));
                expect(body._links.hour.template).toBe(url + '/{year}/{month}/{day}/{hour}');
                expect(body._links.hour.redirect).toBe(url + '/hour');

                expect(body._links.day.href).toBe(channelResource + time.format('/YYYY/MM/DD'));
                expect(body._links.day.redirect).toBe(url + '/day');
                expect(body._links.day.template).toBe(url + '/{year}/{month}/{day}');
                done();
            })
    });


});

