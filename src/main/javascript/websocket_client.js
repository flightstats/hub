/**
 * This is a WebSocket client that logs incoming messages until closed (e.g. CTRL-C).
 */

const {getTimestamp, logParameters} = require('./utils');
const WebSocket = require('ws');
const minimist = require('minimist');
const {URL} = require('url');

const arguments = minimist(process.argv.slice(2));
const urlInput = arguments.url || arguments._[0];
if (!urlInput) throw new Error('You must specify a WebSocket URL (e.g. --url ws://some.where/out/there).');
const url = new URL(urlInput);

logParameters({url: url.href});

const socket = new WebSocket(url.href);

socket.on('message', (message) => {
    console.log(getTimestamp(), 'message:', message);
});

socket.on('error', (error) => {
    console.log(getTimestamp(), error);
});

socket.on('open', () => {
    console.log(getTimestamp(), 'socket opened');
});

socket.on('close', () => {
    console.log(getTimestamp(), 'socket closed');
});

process.on('SIGINT', () => {
    socket.close();
    process.exit();
});
