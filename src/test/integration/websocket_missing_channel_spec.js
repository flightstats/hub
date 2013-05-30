require('./integration_config.js');
jasmine = require('jasmine-node');
var WebSocket = require('ws');

//var url = channelUrl.replace("http", "ws") + "/notgunnafindthisy0/ws";
var url = channelUrl.replace("http", "ws") + "/spoon/ws";

describe("websocket 404", function () {
    it("Verifying that websocket to missing channel returns 404", function () {
        console.log("Connecting to " + url);
        var ws = new WebSocket(url);
        ws.on('open', function (message) {
            console.log("Connected");
            expect(false).toBeTruthy(); //fail
        });
        ws.on('error', function () {
            console.log("I got error")
            //success
        });

    });
});




