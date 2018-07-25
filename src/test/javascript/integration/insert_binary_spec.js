require('../integration_config');
const {
    fromObjectPath,
    getProp,
    hubClientGet,
} = require('../lib/helpers');

const channelName = utils.randomChannelName();
const channelResource = `${channelUrl}/${channelName}`;

describe(__filename, function () {
    it('creates a channel', function (done) {
        var url = channelUrl;
        var headers = {'Content-Type': 'application/json'};
        var body = {'name': channelName};

        utils.httpPost(url, headers, body)
            .then(function (response) {
                expect(getProp('statusCode', response)).toEqual(201);
            })
            .finally(done);
    });

    let imageData;

    it('downloads an image of a cat', async () => {
        const url = 'http://www.lolcats.com/images/u/08/32/lolcatsdotcombkf8azsotkiwu8z2.jpg';
        const headers = {};
        const isBinary = true;

        const response = await hubClientGet(url, headers, isBinary);
        expect(getProp('statusCode', response)).toEqual(200);
        imageData = getProp('body', response) || '';
    });

    let itemURL;

    it('inserts an image into the channel', function (done) {
        var url = channelResource;
        var headers = {'Content-Type': 'image/jpeg'};
        var body = Buffer.from(imageData, 'binary');

        utils.httpPost(url, headers, body)
            .then(function (response) {
                expect(getProp('statusCode', response)).toEqual(201);
                const links = fromObjectPath(['body', '_links'], response) || {};
                const { channel = {}, self = {} } = links;
                expect(channel.href).toEqual(channelResource);
                itemURL = self.href;
            })
            .finally(done);
    });

    it('verifies the image data was inserted correctly', async () => {
        if (!itemURL) return fail('itemURL is not defined by previous test');
        const headers = {};
        const isBinary = true;

        const response = await hubClientGet(itemURL, headers, isBinary);
        const responseBody = getProp('body', response) || '';
        expect(getProp('statusCode', response)).toEqual(200);
        expect(responseBody.length).toEqual(imageData.length);
    });
});
