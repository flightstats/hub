require('../integration/integration_config.js');
var verify_utils = require('./verify_utils.js');
var agent = require('superagent');
var request = require('request');
var async = require('async');
var moment = require('moment');
var _ = require('lodash');
var testName = __filename;

/**
 * This should :
 *
 * 1 - If verifyAlert rule(s) exist,
 *      verify that rule created an item in escalationAlerts
 *      verify that rule has status in latest
 * 2 - delete all rules whose names start with "verifyAlert"
 *
 * 3 - create verifyAlertData channel
 * 4 - create a new rule for channel verifyAlertData, named verifyAlert-{time}
 * 5 - new rule be triggered for a single item on channel verifyAlertData in a minute
 * 6 - add an item to the channel verifyAlertData
 */

//jasmine-node --forceexit --captureExceptions --config hubDomain hub-v2.svc.dev verify_alerts_spec.js

describe(testName, function () {

    var verifyAlertData = hubUrlBase + '/channel/verifyAlertData';
    var existingRules = [];

    verify_utils.getExistingAlerts('verifyAlert', existingRules);

    verify_utils.verifyEscalationAlerts(existingRules);

    it('3 - creates verifyAlertData channel', function (done) {
        console.log('verifyAlertData', verifyAlertData);
        agent
            .put(verifyAlertData)
            .accept('json')
            .send({"ttlDays": "1"})
            .end(function (err, res) {
                expect(err).toBe(null);
                done();
            })
    });

    it('4 - create a new rule for channel verifyAlertData, named verifyAlert-{time}', function (done) {
        var name = 'verifyAlert' + moment().format('YYYY-MM-DD-HH-mm-ss');
        console.log('adding alert', name);
        request.put({
                url: alertUrl + '/' + name,
                headers: {"Content-Type": "application/json"},
                body: JSON.stringify({
                    channel: 'verifyAlertData',
                    threshold: 0,
                    serviceName: 'test',
                    operator: '>',
                    timeWindowMinutes: 1,
                    type: 'channel'
                })
            },
            function (err, response, body) {
                expect(err).toBeNull();
                expect(response.statusCode).toBe(201);
                done();
            });

    }, 60 * 1000);

    it('6 - add an item to the channel verifyAlertData', function (done) {
        console.log('verifyAlertData', verifyAlertData);
        agent
            .post(verifyAlertData)
            .type('json')
            .accept('json')
            .send({name: 'value'})
            .end(function (err, res) {
                expect(err).toBe(null);
                done();
            })
    });

    verify_utils.deleteExisting(existingRules);

});
