require('../integration_config');
const {
    fromObjectPath,
    getProp,
    hubClientGet,
    hubClientPost,
} = require('../lib/helpers');
const {
    getChannelUrl,
} = require('../lib/config');

const channelUrl = getChannelUrl();
const channelName = utils.randomChannelName();
let earliestURL = null;

describe(__filename, function () {
    beforeAll(async () => {
        const headers = { 'Content-Type': 'application/json' };
        const body = { 'name': channelName };
        const response = await hubClientPost(channelUrl, headers, body);
        if (getProp('statusCode', response) === 201) {
            earliestURL = fromObjectPath(['body', '_links', 'earliest', 'href'], response);
        }
    });

    it('verifies the earliest endpoint returns 404 on an empty channel', async () => {
        if (!earliestURL) return fail('required earliestURL not defined by last test');
        const response = await hubClientGet(earliestURL);
        expect(getProp('statusCode', response)).toEqual(404);
    });
});
