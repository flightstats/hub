require('./integration_config.js');
var frisby = require('frisby');

var channelName = "integrationtests" + Math.random().toString().replace(".", "_");
var jsonBody = JSON.stringify({ "name": channelName});
var badValueUrl = channelUrl + "/" + channelName + "/685221b0-77c2-11e2-8a3e-20c9d08600a5";

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


