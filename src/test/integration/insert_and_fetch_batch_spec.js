require('./integration_config.js');

var request = require('request');
var http = require('http');
var channelName = utils.randomChannelName();
var channelResource = channelUrl + '/' + channelName + '/batch';
var testName = __filename;

describe(testName, function () {
    utils.createChannel(channelName);

    var multipart =
        'start...\r\n' +
        '--frontier\r\n' +
        'Content-Type: text/plain\r\n' +
        ' \r\n' +
        'This is the body of the message.\r\n' +
        '--frontier\r\n' +
        'Content-Type: application/octet-stream.\r\n' +
        'Content-Transfer-Encoding: base64\r\n' +
        ' \r\n' +
        'PGh0bWw+CiAgPGhlYWQ+CiAgPC9oZWFkPgogIDxib2R5PgogICAgPHA+VGhpcyBpcyB0aGUgYm9keSBvZiB0aGUgbWVzc2FnZS48L3A+CiAgPC9ib2R5Pgo8L2h0bWw+Cg==\r\n' +
        '--frontier--'

    it("batches items to " + channelResource, function (done) {
        request.post({
                url: channelResource,
                headers: {'Content-Type': "multipart/mixed; boundary=frontier"},
                body: multipart
            },
            function (err, response, body) {
                expect(err).toBeNull();
                console.log('posted', response.body);
                console.log('posted', response.status);
                expect(response.statusCode).toBe(201);
                var parse = JSON.parse(response.body);
                console.log(response.body);
                expect(parse.length).toBe(2);
                done();
            });
    }, 60099);

});

