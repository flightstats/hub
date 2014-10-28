require('./../integration/integration_config.js');

var WebSocket = require('ws');

var url = channelUrl.replace("http", "ws") + "/notgunnafindthisy0xxx/ws";

describe("websocket 404", function () {
    it("Verifying that websocket to missing channel returns 404", function (done) {
        var ws = new WebSocket(url);    //initiates a connection
        ws.on('open', function (message) {
            ws.close();
        });
        ws.on('error', function () {
            expect(true).toBeTruthy(); //just to assert _something_
            done(); //success
        });
    });
});




