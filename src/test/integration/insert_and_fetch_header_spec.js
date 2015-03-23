require('./integration_config.js');

var request = require('request');
var Q = require('q');
var channelName = utils.randomChannelName();
var channelResource = channelUrl + "/" + channelName;
var testName = __filename;

describe(testName, function () {
    utils.createChannel(channelName);

    it('checks accept header', function (done) {

        utils.postItemQ(channelResource)
            .then(function (value) {
                var url = value.body._links.self.href;
                var options = {
                    url: url,
                    headers: {
                        'Accept': 'text/html, image/gif, image/jpeg, *; q=.2, */*; q=.2'
                    }
                };
                request.get(options,
                    function (err, response, body) {
                        expect(err).toBeNull();
                        expect(response.statusCode).toBe(200);
                        done();
                    });


            })
    });

});

