require('../integration/integration_config.js');
var agent = require('superagent');
var request = require('request');
var async = require('async');
var moment = require('moment');
var _ = require('lodash');
var testName = __filename;

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

//jasmine-node --forceexit --captureExceptions --config hubDomain hub-v2.svc.dev verify_max_items_spec.js

describe(testName, function () {

    var verifyMaxItems = hubUrlBase + '/channel/verifyMaxItems';
    var maxItems = 10;

    it('1 - creates verifyMaxItems channel', function (done) {
        agent
            .put(verifyMaxItems)
            .accept('json')
            .send({"maxItems": maxItems})
            .end(function (err, res) {
                expect(err).toBe(null);
                console.log('created', res.body);
                done();
            })
    });

    it('2 - checks for max items', function (done) {
        var checkUrl = verifyMaxItems + '/latest/' + (maxItems * 2);
        console.log('check url ' + checkUrl);
        agent
            .get(checkUrl)
            .end(function (err, res) {
                expect(err).toBe(null);
                expect(res.status).toBe(200);
                expect(res.body._links.uris.length).toBe(maxItems);
                done();
            })

    }, 5 * 60 * 1000);

    it('2 - adds 2 * N items', function (done) {
        //timesLimit(n, limit, iterator, [callback])
        async.timesLimit(2 * maxItems, 5, function (n, callback) {
                utils.postItem(verifyMaxItems, 201, callback)
            },
            function (err, results) {
                console.log('completed adding items');
                done();
            });

    }, 60 * 1000);


});
