require('../integration_config');
const {
    fromObjectPath,
    getProp,
} = require('../lib/helpers');

var request = require('request');
var channelName = utils.randomChannelName();
var channelResource = channelUrl + "/" + channelName;
var testName = __filename;

describe(testName, function () {
    utils.createChannel(channelName);

    it('checks accept header', function (done) {

        utils.postItemQ(channelResource)
            .then(function (value) {
                const url = fromObjectPath(['body', '_links', 'self', 'href'], value);
                var options = {
                    url: url,
                    headers: {
                        'Accept': 'text/html, image/gif, image/jpeg, *; q=.2, */*; q=.2'
                    }
                };
                request.get(options,
                    function (err, response, body) {
                        expect(err).toBeNull();
                        expect(getProp('statusCode', response)).toBe(200);
                        done();
                    });
            });
    });

});
