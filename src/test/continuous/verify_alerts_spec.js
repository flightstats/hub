var agent = require('superagent');
var async = require('async');
var moment = require('moment');
var _ = require('lodash');
var testName = __filename;
var hubUrl = process.env.hubUrl;
hubUrl = 'http://' + hubUrl;
console.log(hubUrl);


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

//jasmine-node --forceexit --captureExceptions --config hubUrl hub-v2.svc.dev verify_alerts_spec.js

describe(testName, function () {

    var escalationAlerts = hubUrl + '/channel/escalationAlerts';
    var verifyAlertData = hubUrl + '/channel/verifyAlertData';
    var zomboAlertsConfig = hubUrl + '/channel/zomboAlertsConfig';
    var zomboAlertStatus = hubUrl + '/channel/zomboAlertStatus';

    var existingRules = [];

    it('1 - if verifyAlert rule(s) exist', function (done) {
        agent
            .get(zomboAlertsConfig + '/latest')
            .accept('json')
            .end(function (err, res) {
                expect(err).toBe(null);
                _.forIn(res.body.insertAlerts, function (value, key) {
                    if (_.startsWith(key, 'verifyAlert')) {
                        console.log('found rule', key);
                        existingRules.push(key);
                    }
                });
                done();
            })
    });

    var escalationAlertsLinks = [];

    it('gets escalationAlerts links', function (done) {
        if (existingRules.length == 0) {
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
                if (_.startsWith(alertItem.description, rule)) {
                    console.log('found alert for rule', found, rule, alertItem);
                    found++;
                }
            });
            expect(found).toBe(1);
        })

        done();
    });

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
        agent
            .get(zomboAlertsConfig + '/latest?stable=false')
            .accept('json')
            .end(function (err, res) {
                expect(err).toBe(null);
                //2 - delete all rules whose names start with "verifyAlert"
                existingRules.forEach(function (name) {
                    delete res.body.insertAlerts[name];
                })
                //5 - new rule be triggered for a single item on channel verifyAlertData in a minute
                res.body.insertAlerts[name] = {
                    channel: 'verifyAlertData', threshold: 0, serviceName: 'test', operator: '>', timeWindowMinutes: 1
                };
                console.log('body', res.body.insertAlerts);
                agent
                    .post(zomboAlertsConfig)
                    .type('json')
                    .accept('json')
                    .send(res.body)
                    .end(function (err, res) {
                        expect(err).toBe(null);
                        done();
                    })
            })
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


});
