const {
    fromObjectPath,
    getProp,
    hubClientGet,
} = require('../lib/helpers');
const {
    getHubUrlBase,
} = require('../lib/config');

const hubUrlBase = getHubUrlBase();

describe(__filename, function () {
    it('verifies the health check returns healthy', async () => {
        const url = `${hubUrlBase}/health`;
        const headers = { 'Content-type': 'application/json' };
        const response = await hubClientGet(url, headers);
        const contentType = response('content-type');
        const healthy = fromObjectPath(['body', 'healthy'], response);
        expect(getProp('statusCode', response)).toEqual(200);
        expect(contentType).toEqual('application/json');
        expect(healthy).toEqual(true);
    });
});
