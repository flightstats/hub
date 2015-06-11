require('./integration_config.js');

var request = require('request');
var alertName = utils.randomChannelName();
var testName = __filename;

var alertConfig = {
    callbackUrl: 'http://nothing/callback',
    channelUrl: 'http://nothing/channel/notHere'
};

/**
 * This should:
 *
 * 1 - create a channel alert
 * 2 - get the channel alert
 * 3 - make sure channel alert shows up in list
 * 4 - delete the alert
 *
 * //todo - gfm - 6/10/15 - use source & channel fields
 */

describe(testName, function () {

    it('creates alert', function (done) {
        console.log('alert url ', alertUrl);
        request.put({
                url: alertUrl + '/' + alertName,
                headers: {"Content-Type": "application/json"},
                body: JSON.stringify(alertConfig)
            },
            function (err, response, body) {

                expect(err).toBeNull();
                expect(response.statusCode).toBe(201);
                console.log('response', body);
                /*if (typeof groupConfig !== "undefined" && status < 400) {
                 var parse = JSON.parse(body);
                 expect(parse.callbackUrl).toBe(groupConfig.callbackUrl);
                 expect(parse.channelUrl).toBe(groupConfig.channelUrl);
                 expect(parse.name).toBe(groupName);
                 }*/
                done();
            });

    });


});

