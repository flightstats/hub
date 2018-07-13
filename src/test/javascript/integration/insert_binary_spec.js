require('../integration_config');
const {
    fromObjectPath,
    getProp,
} = require('../lib/helpers');

var channelName = utils.randomChannelName();
var channelResource = channelUrl + "/" + channelName;

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

    var imageData;

    it('downloads an image of a cat', function (done) {
        var url = 'http://www.lolcats.com/images/u/08/32/lolcatsdotcombkf8azsotkiwu8z2.jpg';
        var headers = {};
        var isBinary = true;

        utils.httpGet(url, headers, isBinary)
            .then(function (response) {
                expect(getProp('statusCode', response)).toEqual(200);
                imageData = getProp('body', response) || '';
            })
            .finally(done);
    });

    var itemURL;

    it('inserts an image into the channel', function (done) {
        var url = channelResource;
        var headers = {'Content-Type': 'image/jpeg'};
        // TODO: "new Buffer()" is deprecated, update this
        var body = new Buffer(imageData, 'binary');

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

    it('verifies the image data was inserted correctly', function (done) {
        var url = itemURL;
        var headers = {};
        var isBinary = true;

        utils.httpGet(url, headers, isBinary)
            .then(function (response) {
                const responseBody = getProp('body', response) || '';
                expect(getProp('statusCode', response)).toEqual(200);
                expect(responseBody.length).toEqual(imageData.length);
            })
            .finally(done);
    });

});
