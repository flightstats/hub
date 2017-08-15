/**
 * This is an HTTP server that accepts requests, doesn't respond, and holds
 * the socket open until the timeout is reached.
 */

const {getTimestamp, logParameters} = require('./utils');
const http = require('http');
const minimist = require('minimist');

const arguments = minimist(process.argv.slice(2));
const port = arguments.port || 54321;
const twoMinutes = 2 * 60 * 1000;
const timeoutInMS = arguments.timeout || twoMinutes;

logParameters({port: port, timeout: `${timeoutInMS}ms`});

const getElapsedSeconds = (start, end) => {
    var elapsedTimeMS = end.getTime() - start.getTime();
    return elapsedTimeMS / 1000;
};

var server = http.createServer((request) => {
    console.log(getTimestamp(), 'connection accepted from:', request.headers.host);

    var start = new Date(), end;
    var closed = false;
    var serverTimedOut = false;

    request.on('close', () => {
        end = new Date();
        closed = true;
    });

    request.socket.on('timeout', () => {
        serverTimedOut = true;
    });

    var intervalID = setInterval(() => {
        if (closed) {
            clearInterval(intervalID);
            console.log(getTimestamp(), (serverTimedOut) ? 'server' : 'client', 'timeout exceeded:', getElapsedSeconds(start, end));
        }
    }, 500);
});

server.setTimeout(timeoutInMS);

process.on('SIGINT', () => {
    server.close(() => {
        console.log(getTimestamp(), 'server stopped');
        process.exit();
    });
});

server.listen(port, () => {
    console.log(getTimestamp(), 'server started');
});
