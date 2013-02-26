require('./integration_config.js');
var utils = require('./utils.js');
var frisby = require('frisby');
var fs = require('fs');
var request = require('request');

var channelName = utils.randomChannelName();
var thisChannelResource = channelUrl + "/" + channelName;

utils.runInTestChannel(channelName, function () {

    catUrl = 'http://www.lolcats.com/images/u/08/32/lolcatsdotcombkf8azsotkiwu8z2.jpg';
    console.info("Fetching some binary content to insert....");
    utils.download(catUrl, function (imagedata) {

        console.info("Inserting an image (" + imagedata.length + " bytes).");

        buf = new Buffer(imagedata, 'binary');
        request.post({url: thisChannelResource, headers: {"Content-Type": "image/jpeg"}, body: buf}, function (error, response, body) {
            expect(error).toBeNull();
            resultObj = JSON.parse(body);
            expect(resultObj['_links']['channel']['href']).toBe(thisChannelResource);

            var valueUrl = resultObj['_links']['self']['href'];
            console.info("Now to retrieve and compare cats: " + valueUrl);
            utils.download(valueUrl, function (hubdata) {
                console.info("Checking to see if the cats match...");
                expect(hubdata.length).toEqual(imagedata.length);
                expect(hubdata).toEqual(imagedata);
                console.info("The cats match!");

            });
        });
    });
});