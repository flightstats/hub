var agent = require('superagent');
var _ = require('lodash');
var async = require('async');
var request = require('request');

function getExistingAlerts(prefix, existingRules) {
    it('1 - if ' + prefix + ' rule(s) exist', function (done) {
        agent
            .get(alertUrl)
            .accept('json')
            .end(function (err, res) {
                expect(err).toBe(null);
                _.forIn(res.body._links.alerts, function (value, key) {
                    if (_.startsWith(value.name, prefix)) {
                        console.log('found rule', value);
                        existingRules.push(value);
                    }
                });
                done();
            })
    }, 60000);
}

function verifyEscalationAlerts(existingRules) {

    var escalationAlertsLinks = [];
    var escalationAlerts = hubUrlBase + '/channel/escalationAlerts';

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
            if (found === 0) {
                console.log('no alert found for rule', rule.name);
            }
            expect(found).toBe(1);
        })

        done();
    });
}

function deleteExisting(existingRules) {
    it('deletes pre-existing rules', function (done) {
        console.log('existing rules', existingRules);
        async.eachSeries(existingRules, function (rule, callback) {
            console.log('deleting rule', rule.href);
            request.del(rule.href,
                function (err, response, body) {
                    expect(err).toBeNull();
                    expect(response.statusCode).toBe(202);
                    console.log('respnse', response.statusCode);
                    callback();
                });
        }, function () {
            done();
        })

    }, 60 * 1000);
}

exports.getExistingAlerts = getExistingAlerts;
exports.verifyEscalationAlerts = verifyEscalationAlerts;
exports.deleteExisting = deleteExisting;
