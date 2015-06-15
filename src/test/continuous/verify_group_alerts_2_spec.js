require('../integration/integration_config.js');
var agent = require('superagent');
var async = require('async');
var request = require('request');
var moment = require('moment');
var _ = require('lodash');
var testName = __filename;
var hubUrl = process.env.hubUrl;
hubUrl = 'http://' + hubUrl;
console.log(hubUrl);


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

//jasmine-node --forceexit --captureExceptions --config hubUrl hub-v2.svc.dev verify_group_alerts_2_spec.js

describe(testName, function () {

    var escalationAlerts = hubUrl + '/channel/escalationAlerts';

    var existingRules = [];

    it('1 - if verifyGroupAlert rule(s) exist', function (done) {
        console.log(hubUrl);
        agent
            .get(alertUrl)
            .accept('json')
            .end(function (err, res) {
                expect(err).toBe(null);
                _.forIn(res.body._links.alerts, function (value, key) {
                    if (_.startsWith(value.name, 'verifyGroupAlert')) {
                        console.log('found rule', value);
                        existingRules.push(value);
                    }
                });
                done();
            })
    });

    var escalationAlertsLinks = [];

    it('gets escalationAlerts links for up to two hours', function (done) {
        if (existingRules.length == 0) {
            console.log('no existing rules found');
            done();
        } else {
            agent
                .get(escalationAlerts + '/time/hour')
                .accept('json')
                .end(function (err, res) {
                    expect(err).toBe(null);
                    escalationAlertsLinks = escalationAlertsLinks.concat(res.body._links.uris);
                    agent
                        .get(res.body._links.previous.href)
                        .accept('json')
                        .end(function (err, res) {
                            expect(err).toBe(null);
                            escalationAlertsLinks = escalationAlertsLinks.concat(res.body._links.uris);
                            console.log('escalationAlertsLinks', escalationAlertsLinks);
                            done();
                        })

                })
        }
    }, 60 * 1000);

    var escalationAlertsItems = [];

    it('loads escalationAlerts items', function (done) {
        async.eachLimit(escalationAlertsLinks, 20,
            function (link, callback) {
                agent
                    .get(link)
                    .accept('json')
                    .end(function (err, res) {
                        expect(err).toBe(null);
                        escalationAlertsItems.push(res.body);
                        callback(err);
                    })

            }, function (err) {
                console.log('escalationAlertsItems ', escalationAlertsItems.length)
                done(err);
            })
    }, 60 * 1000);

    it('looks for escalationAlerts items for existing rules', function (done) {
        existingRules.forEach(function (rule) {
            var found = 0;
            escalationAlertsItems.forEach(function (alertItem) {
                if (_.startsWith(alertItem.description, rule.name)) {
                    console.log('found alert for rule', found, rule.name, alertItem);
                    found++;
                }
            });
            expect(found).toBe(1);
        })

        done();
    });

    it('creates group verifyGroupAlert', function (done) {
        request.put({
                url: hubUrl + '/group/verifyGroupAlert',
                headers: {"Content-Type": "application/json"},
                body: JSON.stringify({
                    callbackUrl: 'http://none',
                    channelUrl: hubUrl + '/channel/load_test_1'
                })
            },
            function (err, response, body) {
                expect(err).toBeNull();
                done();
            });
    });

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


    it('deletes pre-existing rules', function (done) {
        existingRules.forEach(function (rule, index) {
            console.log('deleting rule', rule.href);
            request.del(rule.href,
                function (err, response, body) {
                    expect(err).toBeNull();
                    expect(response.statusCode).toBe(202);
                    console.log('respnse', response.statusCode);
                    if (index === existingRules.length - 1) {
                        done();
                    }
                });
        })
    });

});
