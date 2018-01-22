require('../integration_config');

var channelName = '123_you_aint_gunna_find_me';
var channelResource = channelUrl + "/" + channelName;
var messageText = "Any old value!";

describe(__filename, function () {

    it('inserts an item into a bogus channel', function (done) {
        var url = channelResource;
        var headers = {'Content-Type': 'text/plain'};
        var body = messageText;

        utils.httpPost(url, headers, body)
            .then(function (response) {
                expect(response.statusCode).toEqual(404);
            })
            .finally(done);
    });


});
