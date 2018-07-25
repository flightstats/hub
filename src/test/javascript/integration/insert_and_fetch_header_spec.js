require('../integration_config');
const {
    createChannel,
    fromObjectPath,
    getProp,
} = require('../lib/helpers');

var request = require('request');
var channelName = utils.randomChannelName();
const channelResource = `${channelUrl}/${channelName}`;
var testName = __filename;
let createdChannel = false;

describe(testName, function () {
    beforeAll(async () => {
        const channel = await createChannel(channelName);
        if (getProp('statusCode', channel) === 201) {
            createdChannel = true;
        }
    });

    it('checks accept header', function (done) {
        if (!createdChannel) return done.fail('channel not created in before block');
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
