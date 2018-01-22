require('../integration_config.js');
var async = require('async');
var moment = require('moment');
var testName = __filename;
var locustUrl = process.env.locustUrl;
locustUrl = 'http://' + locustUrl + '/stats';
console.log(locustUrl);

var timeout = 5 * 60 * 1000;
/**
 * This should get the results from a running locust install and logs results to the console
 * http://locustUrl/stats/requests
 * it should also reset the stats.
 * http://locustUrl/stats/reset
 */
describe(testName, function () {

    var results;

    it('loads results', function (done) {
        utils.httpGet(`${locustUrl}/requests`, {'Accept': 'application/json'})
            .then(response => {
                console.log('response:', response.body);
                response.body.stats.forEach(function (item) {
                    console.log(`item ${item.name} ${item.num_failures}`);
                    expect(item.num_failures).toBe(0);
                });
            })
            .finally(done);
    }, timeout);

    it('resets stats', function (done) {
        utils.httpGet(`${locustUrl}/reset`)
            .then(response => {
                expect(response.statusCode).toBe(200);
            })
            .finally(done);
    }, timeout);

});
