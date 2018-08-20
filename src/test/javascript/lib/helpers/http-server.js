const http = require('http');
const https = require('https');
const { readFileSync } = require('fs');
const { getCallBackDomain } = require('../config');

const getHttps = () => new https.Server({
    key: readFileSync('localhost.key'),
    cert: readFileSync('localhost.cert'),
});
const getHttp = () => new http.Server();

const startServer = async (port, callback, secure) => {
    const server = secure ? getHttps() : getHttp();

    server.on('connection', (socket) => {
        socket.setTimeout(1000);
    });

    server.on('request', (request, response) => {
        let incoming = '';

        request.on('data', (chunk) => {
            incoming = `${incoming}${chunk.toString()}`;
        });

        request.on('end', () => {
            if (callback) callback(incoming, response);
            response.end();
        });
    });

    server.listen(port);
    const listeningServer = await server.on('listening', () => {
        console.log(`server listening at ${getCallBackDomain()}:${port}/`);
        return server;
    });
    return listeningServer;
};

const defaultCallback = () => {};
const closeServer = async (server, callback = defaultCallback) => {
    if (!server || !server.address) {
        console.log('ERROR: server arg passed to closeServer is not valid: ', server);
        return false;
    }
    const { port } = server.address();
    await server.close(callback);
    console.log('closed server on port', port);
};

module.exports = { closeServer, startServer };
