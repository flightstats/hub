/*
 * Super simple websocket channel client.
 */
var WebSocket = require('ws');

function usage() {
    console.log("Usage:", process.argv[0], process.argv[1], "<channel>")
}

if (process.argv.length != 3) {
    usage();
    process.exit()
}

var channelName = process.argv[2];
var url = 'ws://localhost:8080/channel/' + channelName + '/ws';

console.log("Connecting to " + url);
var ws = new WebSocket(url);

ws.on('open', function () {
    console.log("Connected.")
});
ws.on('message', function (message) {
    console.log('received: %s', message);
});
