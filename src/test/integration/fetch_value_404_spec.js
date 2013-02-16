require('./integration_config.js');
var frisby = require('frisby');

var channelName = "integrationtests";
var jsonBody = JSON.stringify({ "name": channelName});
var badValueUrl = channelUrl + "/" + channelName + "/foooo" + Math.random().toString();

console.info('Ensuring that test channel has been created...');
frisby.create('Ensuring that the test channel exists.')
    .post(channelUrl, null, { body: JSON.stringify({ "name": channelName})})
    .addHeader("Content-Type", "application/json")
    .toss();

console.info('Fetching a nonexistent value...');
frisby.create('Fetching a nonexistent value.')
    .get(badValueUrl)
    .expectStatus(404)
    .toss();


