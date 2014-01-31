/*
 * Super simple websocket channel client.
 */
var WebSocket = require('ws');

function usage() {
    console.log("Usage:", process.argv[0], process.argv[1], "<host[:port]> <channel>")
}

if (process.argv.length != 4) {
    usage();
    process.exit()
}

var hostPort = process.argv[2];
var channelName = process.argv[3];
var url = 'ws://' + hostPort + '/channel/' + channelName + '/ws';

console.log("Connecting to " + url);
var ws = new WebSocket(url);

ws.on('open', function () {
    console.log("Connected.")
});
ws.on('close', function () {
    console.log("Disconnected")
});
ws.on('error', function (error) {
    console.log("Error: " + error)
});
ws.on('message', function (message) {
    console.log('received: %s', message);
});
