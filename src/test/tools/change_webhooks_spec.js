require('../integration/integration_config.js');

var agent = require('superagent');
var request = require('request');
var async = require('async');
var moment = require('moment');
var _ = require('lodash');
var testName = __filename;
var MINUTE = 60 * 1000;

var hubUrl = 'https://' + process.env.hubUrl;
var oldUrl = 'https://' + process.env.oldUrl;

/**
 * Usage:
 *
 * jasmine-node --forceexit --captureExceptions --config hubUrl hub --config oldUrl old-hub change_webhooks_spec.js
 *
 * This should :
 *
 * For a given hub
 * get all the webhooks
 * if a webhook has an old style channelUrl, update it to the new style
 */

describe(testName, function () {

    var webhooksToChange = {};

    it('loads ' + hubUrl + ' webhooks  ', function (done) {
        console.log('running with hubUrl=' + hubUrl + ' oldUrl=' + oldUrl);
        agent.get(hubUrl + '/internal/webhook/configs')
            .set('Accept', 'application/json')
            .end(function (res) {
                expect(res.error).toBe(false);
                var webhooks = res.body.webhooks;
                console.log('found webhooks', webhooks);
                webhooks.forEach(function (webhook) {
                    if (_.startsWith(webhook.channelUrl, oldUrl)) {
                        console.log('adding webhook ', webhook.name);
                        webhooksToChange[webhook.name] = webhook;
                    } else {
                        console.log('excluding webhook ', webhook.name);
                    }
                });

                done();
            })
    }, MINUTE);

    it('gets updates webhooks ', function (done) {
        async.eachLimit(webhooksToChange, 2,
            function (webhook, callback) {
                var newUrl = webhook.channelUrl.replace(oldUrl, hubUrl);
                console.log('newUrl ', newUrl, webhook);
                request.put({
                        url: hubUrl + '/webhook/' + webhook.name,
                        headers: {"Content-Type": "application/json"},
                        body: {
                            channelUrl: newUrl
                        },
                        json: true
                    },
                    function (err, response, body) {
                        expect(err).toBeNull();
                        expect(response.statusCode).toBe(200);

                        console.log('response', body);
                        expect(body.channelUrl).toBe(newUrl);
                        callback();
                    });
            }, function (err) {
                done(err);
            });
    }, MINUTE);

});
