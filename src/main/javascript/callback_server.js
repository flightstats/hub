/**
 * This is an HTTP server that logs incoming messages until closed (e.g. CTRL-C).
 */

const {getTimestamp, logParameters} = require('./utils');
const http = require('http');
const minimist = require('minimist');

const arguments = minimist(process.argv.slice(2));
const port = arguments.port || 54321;

logParameters({
    port: port
});

var server = http.createServer((request, response) => {
    console.log(getTimestamp(), 'connection open:', request.headers.host);

    var payload = '';

    request.on('data', (chunk) => {
        payload += chunk.toString();
    });

    request.on('end', () => {
        console.log(getTimestamp(), 'received: ' + payload);
    });

    request.on('close', () => {
        console.log(getTimestamp(), 'connection closed:', request.headers.host);
    });

    response.setHeader('Strict-Transport-Security', 'max-age=31536000; includeSubDomains');
    response.writeHead(200);
    response.end('ok');

});

process.on('SIGINT', () => {
    server.close(() => {
        console.log(getTimestamp(), 'server stopped');
        process.exit();
    });
});

server.listen(port, () => {
    console.log(getTimestamp(), 'server started');
});
