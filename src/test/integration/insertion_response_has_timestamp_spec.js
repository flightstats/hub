var utils = require('./utils.js');
var frisby = require('frisby');

var channelName = "integrationtests";
var thisChannelResource = channelUrl + "/" + channelName;
var messageText = "MY SUPER TEST CASE: this & <that>. " + Math.random().toString();

utils.ensureTestChannelCreated(channelName);

console.info('Inserting a value...');
frisby.create('Inserting a value into a channel.')
    .post(thisChannelResource, null, { body: messageText})
    .addHeader("Content-Type", "text/plain")
    .expectStatus(200)
    .expectHeader('content-type', 'application/json')
    .expectJSON('_links', {
        channel: {
            href: thisChannelResource
        }
    })
    .expectJSON('_links.self', {
        href: function (value) {
            var regex = new RegExp("^" + thisChannelResource.replace(/\//g, "\\/").replace(/\:/g, "\\:") + "\\/[a-f,0-9]{8}-[a-f,0-9]{4}-[a-f,0-9]{4}-[a-f,0-9]{4}-[a-f,0-9]{12}$");
            expect(value).toMatch(regex);
        }
    })
    .expectJSON({
        id: function (value) {
            expect(value).toMatch(/^[a-f,0-9]{8}-[a-f,0-9]{4}-[a-f,0-9]{4}-[a-f,0-9]{4}-[a-f,0-9]{12}$/);
        },
        timestamp: function (value) {
            expect(value).toMatch(/^\d\d\d\d-\d\d-\d\dT\d\d:\d\d:\d\d\.\d\d\d-\d\d:\d\d$/);
        }
    })
    .inspectJSON()
    .toss();

