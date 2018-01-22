require('../integration_config');

var channelName = "no_way_jose90928280xFF";
var channelResource = channelUrl + "/" + channelName;

describe(__filename, function () {

    it('gets channel metadata for a nonexistent channel', function (done) {
        var url = channelResource;
        var headers = {};
        var body = '';

        // TODO: i don't think POST is the correct verb for this test
        // I'm keeping it as POST for now though since thats what the
        // Frisby test did.

        utils.httpPost(url, headers, body)
            .then(function (response) {
                expect(response.statusCode).toEqual(404);
            })
            .finally(done);
    });

});
