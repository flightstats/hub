require('../integration_config');
const {
    fromObjectPath,
    getProp,
    hubClientGet,
} = require('../lib/helpers');

const channelName = utils.randomChannelName();
const channelResource = channelUrl + "/" + channelName;
const headers = { 'Content-Type': 'application/json' };
describe(__filename, function () {
    it('verifies the channel doesn\'t exist yet', async () => {
        const response = await hubClientGet(channelResource);
        expect(getProp('statusCode', response)).toEqual(404);
    });

    it('creates a channel with valid storage', function (done) {
        var url = channelUrl;
        var body = {'name': channelName, 'storage': 'BOTH'};

        utils.httpPost(url, headers, body)
            .then(function (response) {
                expect(getProp('statusCode', response)).toEqual(201);
                const contentType = fromObjectPath(['headers', 'content-type'], response);
                const storage = fromObjectPath(['body', 'storage'], response);
                expect(contentType).toEqual('application/json');
                expect(storage).toEqual('BOTH');
            })
            .finally(done);
    });

    it('verifies the channel does exist', async () => {
        const response = await hubClientGet(channelResource);
        console.log('response', response);
        expect(getProp('statusCode', response)).toEqual(200);
        const contentType = fromObjectPath(['headers', 'content-type'], response);
        const storage = fromObjectPath(['body', 'storage'], response);
        const name = fromObjectPath(['body', 'name'], response);
        expect(contentType).toEqual('application/json');
        expect(name).toEqual(channelName);
        expect(storage).toEqual('BOTH');
    });
});
