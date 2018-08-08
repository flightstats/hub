require('../integration_config');
const { getProp } = require('../lib/helpers');

var channelName = utils.randomChannelName();
const channelResource = `${channelUrl}/${channelName}`;

describe(__filename, function () {

    it('creates a channel with no information', function (done) {
        const url = channelResource;
        const headers = {'Content-Type': 'application/json'};
        const body = {}; // equiv to body = '' in request js which now requires min empty object as body arg when json = true.
        utils.httpPut(url, headers, body)
            .then(function (response) {
                expect(getProp('statusCode', response)).toEqual(201);
            })
            .finally(done);
    });

});
