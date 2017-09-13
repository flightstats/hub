require('../integration_config');

const request = require('request');

/**
 * POST a large item, GET it, and verify the "X-Item-Length"
 * header is present with the correct value
 */

describe(__filename, function () {
    var channelName = utils.randomChannelName();
    var channelEndpoint = channelUrl + '/' + channelName;
    var itemHeaders = {'Content-Type': 'text/plain'};
    var itemSize = 41 * 1024 * 1024;
    var itemContent = Array(itemSize).join('a');
    var itemURL;
    var executeTest = true;

    it("checks the hub for large item suport", function (done) {
        request.get({
                url: hubUrlBase + '/internal/properties'
            },
            function (err, response, body) {
                expect(err).toBeNull();
                expect(response.statusCode).toBe(200);
                var parse = utils.parseJson(response);
                console.log(response.body);
                var hubType = parse['properties']['hub.type'];
                executeTest = hubType === 'aws';
                console.log(hubType, 'executeTest', executeTest);
                done();
            });
    });

    utils.createChannel(channelName, null, 'large inserts');

    it('posts a large item', function (done) {
        if (!executeTest) pending();

        utils.postItemQwithPayload(channelEndpoint, itemHeaders, itemContent)
            .then(function (result) {
                expect(function () {
                    var json = JSON.parse(result.body);
                    itemURL = json._links.self.href;
                }).not.toThrow();
                done();
            });
    });

    it('verifies item has correct length info', function (done) {
        if (!executeTest) pending();

        expect(itemURL !== undefined).toBe(true);
        utils.getItem(itemURL, function (headers, body) {
            console.log('headers:', headers);
            expect('x-item-length' in headers).toBe(true);
            var bytes = itemSize - 1; // not sure why the -1 is needed. stole this from insert_and_fetch_large_spec.js
            expect(headers['x-item-length']).toBe(bytes.toString());
            //expect(body.toString()).toEqual(itemContent);
            done();
        });
    }, 5 * 60 * 1000);

});
