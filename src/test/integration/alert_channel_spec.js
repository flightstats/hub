require('./integration_config.js');

var request = require('request');
var alertName = utils.randomChannelName();
var testName = __filename;

var alertConfig = {

};

/**
 * This should:
 *
 * 1 - create a channel alert
 * 2 - get the channel alert
 * 3 - make sure channel alert shows up in list
 * 4 - delete the alert
 *
 * //todo - gfm - 6/10/15 - use source & channel field names
 */

describe(testName, function () {

    it('creates alert', function (done) {
        request.put({
                url: alertUrl + '/' + alertName,
                headers: {"Content-Type": "application/json"},
                body: JSON.stringify(alertConfig)
            },
            function (err, response, body) {
                expect(err).toBeNull();
                expect(response.statusCode).toBe(201);
                body = JSON.parse(body);
                console.log('response', body);
                expect(body.name).toBe(alertName);
                //todo - gfm - 6/11/15 - verify
                done();
            });

    });

    it('checks for alert in config', function (done) {
        request.get({url: alertUrl},
            function (err, response, body) {
                expect(err).toBeNull();
                expect(response.statusCode).toBe(200);
                body = JSON.parse(body);
                var alerts = body['_links']['alerts'];
                expect(alerts.length).toBeGreaterThan(0);
                var found = false;
                for (var i in alerts) {
                    var alert = alerts[i];
                    if (alert.name === alertName) {
                        found = true;
                    }
                }
                if (!found) {
                    expect("alerts to contain ").toBe(alertName);
                }
                done();
            });

    });


});

