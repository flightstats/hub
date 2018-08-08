require('../integration_config');
const {

  getProp,
} = require('../lib/helpers');

var request = require('request');
var channelName = utils.randomChannelName();
const channelResource = `${channelUrl}/${channelName}`;
var testName = __filename;

/**
 * create a channel
 * post malformed bulk payload
 * get back an error
 */
describe(testName, function () {

    utils.putChannel(channelName, false, {"name": channelName, "ttlDays": 1, "tags": ["bulk"]});

    function post_malheur(payload) {
        it("posts malformed item to " + channelResource, function (done) {
            request.post({
                    url: channelResource + '/bulk',
                    headers: {'Content-Type': "multipart/mixed; boundary=abcdefg"},
                    body: payload
                },
                function (err, response, body) {
                    expect(err).toBeNull();
                    expect(getProp('statusCode', response)).toBe(400);
                    console.log(getProp('body', response));
                    done();
                });
        });
    }

    post_malheur('--abcdefg--');

    post_malheur('--abcdefg\r\n' + '--abcdefg--');

});
