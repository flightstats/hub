require('../integration/integration_config.js');
var verify_utils = require('./verify_utils.js');
var agent = require('superagent');
var async = require('async');
var request = require('request');
var moment = require('moment');
var _ = require('lodash');
var testName = __filename;


/**
 * This presumes a channel is constantly creating data, load_test_1
 *
 * This should :
 *
 * 1 - If verifyGroupAlert rule(s) exist,
 *      verify that rule created an item in escalationAlerts
 *      verify that rule has status in latest
 * 2 - delete all rules whose names start with "verifyGroupAlert"
 *
 * 4 - create group callback
 * 5 - create a new rule for channel load_test_1, named verifyGroupAlert-{time}
 */

//jasmine-node --forceexit --captureExceptions --config hubDomain hub-v2.svc.dev verify_group_alerts_spec.js

describe(testName, function () {

    var existingRules = [];

    verify_utils.getExistingAlerts('verifyGroupAlert', existingRules);

    verify_utils.verifyEscalationAlerts(existingRules);

    utils.putGroup('verifyGroupAlert', {
        callbackUrl: 'http://none',
        channelUrl: hubUrlBase + '/channel/load_test_1'
    }, 200);

    it('4 - create a new rule for group , named verifyGroupAlert-{time}', function (done) {
        var name = 'verifyGroupAlert' + moment().format('YYYY-MM-DD-HH-mm-ss');
        console.log('adding group alert', name);
        request.put({
                url: alertUrl + '/' + name,
                headers: {"Content-Type": "application/json"},
                body: JSON.stringify({
                    source: 'verifyGroupAlert',
                    serviceName: 'test',
                    timeWindowMinutes: 1,
                    type: 'group'
                })
            },
            function (err, response, body) {
                expect(err).toBeNull();
                expect(response.statusCode).toBe(201);
                done();
            });
    });

    verify_utils.deleteExisting(existingRules);

});
