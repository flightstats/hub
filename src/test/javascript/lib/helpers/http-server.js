const express = require('express');
const http = require('http');
const https = require('https');
const bodyParser = require('body-parser');
const { readFileSync } = require('fs');
const { fromObjectPath } = require('./functional');
const { getCallBackDomain } = require('../config');

const creds = {
    key: readFileSync('localhost.key'),
    cert: readFileSync('localhost.cert'),
};
const getHttps = app => new https.Server(creds, app);
const getHttp = app => new http.Server(app);

const startServer = async (port, callback, path = '/', secure, file) => {
    const app = express();

    app.use(bodyParser.json());

    app.post(path, (request, response) => {
        // console.log('request.body', request.body);
        const arr = fromObjectPath(['body', 'uris'], request) || [];
        const str = arr[arr.length - 1] || '';
        if (callback) callback(str, response);
    });

    const server = secure ? getHttps(app) : getHttp(app);

    server.on('connection', (socket) => {
        socket.setTimeout(1000);
    });

    server.on('request', function (request, response) {
        if (file === '/home/integration/webhook_error_channel_spec.js') {
            console.log('request.headers', request.headers);
        }
        request.on('end', function () {
            response.end();
        });
    });

    server.listen(port);
    const listeningServer = await server.on('listening', () => {
        console.log(`server listening at ${getCallBackDomain()}:${port}${path}`);
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
