require('../integration_config');
const { getProp } = require('../lib/helpers');

var channelResource = channelUrl + '/nonExistent';

describe(__filename, function () {
    it('deletes a channel that doesn\'t exist', function (done) {
        var url = channelResource;
        var headers = {'Content-Type': 'application/json'};

        utils.httpDelete(url, headers)
            .then(function (response) {
                expect(getProp('statusCode', response)).toEqual(404);
            })
            .finally(done);
    });
});
