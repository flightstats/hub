/**
 * Submits a request to the supplied URL every N milliseconds.
 */

const {getTimestamp, logParameters} = require('./utils');
const http = require('http');
const minimist = require('minimist');
const {URL} = require('url');

const arguments = minimist(process.argv.slice(2));
const urlInput = arguments.url || arguments._[0];
if (!urlInput) throw new Error('You must specify a consumer URL (e.g. --url http://some.where/out/there).');
const url = new URL(urlInput);
const oneSecondInMS = 1000;
const defaultFrequency = oneSecondInMS;

// Validate the frequency input
let frequency = parseInt(arguments.frequency, 10);
if (isNaN(frequency) || frequency <= 0) {
    frequency = defaultFrequency;
}

logParameters({
    url: url.href,
    frequency: `${frequency}ms`
});

const options = {
    host: url.hostname,
    port: url.port,
    path: url.pathname,
    method: 'POST',
    headers: {'Content-Type': 'text/plain'}
};

const provide = () => {
    var request = http.request(options, (response) => {
        var payload = '';

        response.on('data', (chunk) => {
            payload += chunk.toString();
        });

        response.on('end', () => {
            console.log(getTimestamp(), '< POST', response.statusCode, payload);
        });
    });

    var payload = Math.random().toString(36).slice(2);
    console.log(getTimestamp(), '> POST', payload);
    request.write(payload);
    request.end();
};

provide();
setInterval(provide, frequency);

process.on('SIGINT', () => {
    process.exit();
});
