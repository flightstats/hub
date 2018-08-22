// This test is intended to be run as a jenkins scheduled "continuous" test on pdx dev
require('../integration_config');

var request = require('request');
const { getChannelUrl } = require('../lib/config');
var channelName = "destination";
const channelResource = `${getChannelUrl()}/${channelName}`;
var testName = __filename;
var webhookurl = "http://hub.iad.dev.flightstats.io/webhook/Repl_hub_ucs_dev_destination";
var webhookName = "Repl_hub_ucs_dev_destination";
var pause = null;
var webhookConfig = null;

describe(testName, function () {
    utils.getChannel(channelName, null, channelName, "http://hub.pdx.dev.flightstats.io");
    it('verify webhook ', function (done) {
        request.get({
                url: webhookurl,
                headers: {"Content-Type": "application/json"}
            },
            function (err, response, body) {
                expect(err).toBeNull();
                expect(response.statusCode).toBe(200);
                webhookConfig = JSON.parse(body);
                pause = webhookConfig.paused;
                console.log("is it paused? ", pause);
                webhookConfig.paused = !pause;
                done();
            });
    });

    it('creates group ' + webhookName, function (done) {
        console.log('creating group ' + webhookName + ' for ' + testName);
        request.put({
                url: webhookurl,
                headers: {"Content-Type": "application/json"},
                body: JSON.stringify(webhookConfig)
            },
            function (err, response, body) {
                expect(err).toBeNull();
                var parse = utils.parseJson(response);
                expect(parse.paused).toBe(!pause);
                done();
            });
    });
});
