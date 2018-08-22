require('../integration_config');
var request = require('request');
var async = require('async');
var moment = require('moment');
var testName = __filename;
const { getHubUrlBase } = require('../lib/config');
/**
 * This is designed to run once a day.
 * It should:
 *
 * 1 - Create channel
 * 2 - Verify that the channel has the correct number of items in it.
 * * THis will fail the first time you run it.adding
 * 3 - add more items.
 *
 */

describe(testName, function () {

    var verifyMaxItems = getHubUrlBase() + '/channel/verifyMaxItems';
    var maxItems = 10;

    it('1 - creates verifyMaxItems channel', function (done) {
        let headers = {'Accept': 'application/json'};
        let body = {'maxItems': maxItems};
        utils.httpPut(verifyMaxItems, headers, body)
            .then(res => {
                console.log('created', res.body);
            })
            .finally(done);
    });

    it('2 - checks for max items', function (done) {
        var checkUrl = verifyMaxItems + '/latest/' + (maxItems * 2);
        console.log('check url ' + checkUrl);
        utils.httpGet(checkUrl)
            .then(res => {
                expect(res.statusCode).toBe(200);
                expect(res.body._links.uris.length).toBe(maxItems);
            })
            .finally(done);

    });

    it('2 - adds 2 * N items', function (done) {
        //timesLimit(n, limit, iterator, [callback])
        async.timesLimit(2 * maxItems, 5, function (n, callback) {
                utils.postItem(verifyMaxItems, 201, callback)
            },
            function (err, results) {
                console.log('completed adding items');
                done();
            });

    });


});
