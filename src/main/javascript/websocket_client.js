var url = process.argv[2];
if (url === undefined || process.argv.length > 3) {
    console.log('You must supply a websocket URL as the only parameter');
    process.exit(1);
}

var WebSocket = require('ws');
var webSocket = new WebSocket(url);
var messages = [];

webSocket.on('message', function (message) {
    messages.push(message);
});

webSocket.on('error', function (error) {
    console.log('websocket error:', error);
});

webSocket.on('open', function () {
    console.log('websocket opened:', url);
});

webSocket.on('close', function () {
    console.log('websocket closed');
});

function waitForData(delayInMS) {
    setTimeout(function () {
        if (messages.length > 0) {
            console.log('data received:', messages);
            webSocket.close();
        } else {
            waitForData(delayInMS);
        }
    }, delayInMS);
}

waitForData(1000);
