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
    var foundHrefs = [];
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
    });

    waitsFor(function () {
        return hrefs.length == 3;
    }, 5000);

    function getHrefs(format) {
        request.get({url: hubUrlBase + "/channel/" + channelName + "/time/" + format},
            function (err, response, body) {
                expect(err).toBeNull();
                resultObj = JSON.parse(body);
                resultObj._links.uris.forEach(function (item) {
                    foundHrefs.push(item);
                });
            });
    }

    runs(function () {
        var now = moment().add('minutes', -1);
        var format = 'YYYY-MM-DDTHH:mmZ';
        getHrefs(now.format(format));
        getHrefs(now.add('minutes', 1).format(format));
        getHrefs(now.add('minutes', 1).format(format));
    });

    waitsFor(function () {
        return foundHrefs.length == 3;
    }, 10000);

    runs(function () {
        hrefs.forEach(function (item) {
            expect(foundHrefs.indexOf(item)).not.toBe(-1);
        })
    });

});
