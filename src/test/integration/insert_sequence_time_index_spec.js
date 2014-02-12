require('./integration_config.js');
var fs = require('fs');
//npm install --save moment
var moment = require('moment');
var request = require('request');
var testName = "insert_sequence_time_index_spec";
var channelName = utils.randomChannelName();
var thisChannelResource = channelUrl + "/" + channelName;
var messageText = "MY SUPER TEST CASE: this & <that>. " + Math.random().toString();
var channelRequest = JSON.stringify({ "name": channelName });

utils.configureFrisby();

utils.runInTestChannelJson(channelRequest, function () {
    var hrefs = [];
    function insert() {
        request.post({url: thisChannelResource, headers: {"Content-Type": "text/plain"}, body: messageText}, function (err, response, body) {
            expect(err).toBeNull();
            resultObj = JSON.parse(body);
            hrefs.push(resultObj['_links']['self']['href']);
        });
    }

    runs(function () {
        insert();
        insert();
        insert();
    })

    waitsFor(function () {
        return hrefs.length == 3;
    }, 5000);

    function verify(result) {
        var uris = result['_links']['uris'];
        hrefs.forEach(function (item) {
            expect(uris.indexOf(item)).not.toBe(-1);
        })
    }

    runs(function () {
        var format = moment().format("YYYY-MM-DDTHH:mmZ");

        frisby.create(testName + ': Fetching ids with time.')
            .get(hubUrlBase + "/channel/" + channelName + "/time/" + format)
            .expectStatus(200)
            .expectHeader('content-type', 'application/json')
            .afterJSON(function (result) {
                verify(result);
            })
            .toss();


    });

});
