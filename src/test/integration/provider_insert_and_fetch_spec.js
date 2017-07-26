require('./../integration/integration_config.js');

var channelName = utils.randomChannelName();
var providerResource = hubUrlBase + "/provider";
var channelResource = channelUrl + "/" + channelName;
var messageText = "MY SUPER TEST CASE: this & <that>. " + Math.random().toString();

describe(__filename, function () {

    it('inserts a value into a provider channel', function (done) {
        var url = providerResource;
        var headers = {
            'channelName': channelName,
            'Content-Type': 'text/plain'
        };
        var body = messageText;

        utils.httpPost(url, headers, body)
            .then(function (response) {
                expect(response.statusCode).toEqual(200);
            })
            .catch(function (error) {
                expect(error).toBeNull();
            })
            .fin(function () {
                done();
            });
    });

    it('verifies the value was inserted', function (done) {
        var url = channelResource + '/latest?stable=false';

        utils.httpGet(url)
            .then(function (response) {
                expect(response.statusCode).toEqual(200);
                expect(response.headers['content-type']).toEqual(text/plain);
                expect(response.body).toContain(messageText);
            })
            .catch(function (error) {
                expect(error).toBeNull();
            })
            .fin(function () {
                done();
            });
    });

});
