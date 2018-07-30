require('../integration_config');
const { getProp, hubClientDelete } = require('../lib/helpers');

var channelResource = channelUrl + '/nonExistent';

describe(__filename, function () {
    it('deletes a channel that doesn\'t exist', async () => {
        const url = channelResource;
        const headers = {'Content-Type': 'application/json'};

        const response = await hubClientDelete(url, headers);
        expect(getProp('statusCode', response)).toEqual(404);
    });
});
