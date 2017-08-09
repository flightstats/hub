require('../integration_config.js');
var async = require('async');
var moment = require('moment');
var testName = __filename;
var locustUrl = process.env.locustUrl;
locustUrl = 'http://' + locustUrl + '/stats/';
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
        var url = locustUrl + 'requests';
        let headers = {'Accept': 'application/json'};
        console.log('url', url);
        utils.httpGet(url, headers)
            .then(res => {
                results = JSON.parse(res.text);
                console.log('results', results);
                results.stats.forEach(function (item) {
                    console.log('item ' + item.name + ' ' + item.num_failures);
                    expect(item.num_failures).toBe(0);
                });
            })
            .catch(error => {
                expect(error).toBeNull();
            })
            .fin(done);
    }, timeout);

    it('resets stats', function (done) {
        var url = locustUrl + 'reset';
        let headers = {'Accept': 'application/json'};
        console.log('url', url);
        utils.httpGet(url, headers)
            .then(res => {
                expect(res.status).toBe(200);
            })
            .catch(error => {
                expect(error).toBeNull();
            })
            .fin(done);
    }, timeout);

});
