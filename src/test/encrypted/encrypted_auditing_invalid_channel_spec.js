require('./../integration/integration_config.js');

var request = require('request');
var channelName = utils.randomChannelName() + '_audit';
var channelResource = channelUrl + "/" + channelName;
var testName = __filename;

if (!runEncrypted) return;


describe(testName, function () {
    it('can not create auditing channel', function (done) {
        request.post({url : channelUrl,
                headers : {"Content-Type" : "application/json"},
                body : JSON.stringify({ "name" : channelName })},
            function (err, response, body) {
                expect(err).toBeNull();
                expect(response.statusCode).toBe(403);
                done();
            });
    });


});

