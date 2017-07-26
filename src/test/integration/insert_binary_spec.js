require('./integration_config.js');

var channelName = utils.randomChannelName();
var channelResource = channelUrl + "/" + channelName;

describe(__filename, function () {

    it('creates a channel', function (done) {
        var url = channelUrl;
        var headers = {'Content-Type': 'application/json'};
        var body = {};
        
        utils.httpPost(url, headers, body)
            .then(function (response) {
                expect(response.statusCode).toEqual(201);
            })
            .catch(function (error) {
                expect(error).toBeNull();
            })
            .fin(function () {
                done();
            });
    });

    var imageData;
    
    it('downloads an image of a cat', function (done) {
        var url = 'http://www.lolcats.com/images/u/08/32/lolcatsdotcombkf8azsotkiwu8z2.jpg';

        utils.httpGet(url)
            .then(function (response) {
                expect(response.statusCode).toEqual(200);
                imageData = response.body;
            })
            .catch(function (error) {
                expect(error).toBeNull();
            })
            .fin(function () {
                done();
            });
    });

    var itemURL;

    it('inserts an image into the channel', function (done) {
        var url = channelResource;
        var headers = {'Content-Type': 'image/jpeg'};
        var body = new Buffer(imageData, 'binary');

        utils.httpPost(url, headers, body)
            .then(function (response) {
                expect(response.statusCode).toEqual(201);
                expect(response.body._links.channel.href).toEqual(channelResource);
                itemURL = response.body._links.self.href;
            })
            .catch(function (error) {
                expect(error).toBeNull();
            })
            .fin(function () {
                done();
            });
    });

    it('verifies the image data was inserted correctly', function (done) {
        utils.httpGet(itemURL)
            .then(function (response) {
                expect(response.statusCode).toEqual(200);
                expect(response.body.length).toEqual(imageData.length);
            })
            .catch(function (error) {
                expect(error).toBeNull();
            })
            .fin(function () {
                done();
            });
    });

});
