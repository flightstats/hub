require('./integration_config.js');
var fs = require('fs');
var request = require('request');
var channelName = utils.randomChannelName();
var thisChannelResource = channelUrl + "/" + channelName;
var testName = __filename;

utils.runInTestChannel(testName, channelName, function () {

    catUrl = 'http://www.lolcats.com/images/u/08/32/lolcatsdotcombkf8azsotkiwu8z2.jpg';

    var error;
    var comparisonHasBeenFetched = false;
    var returnedHref = "";
    var hubdata = [1];
    var imagedata = [0];

    runs(function () {
        utils.download(catUrl, function (imgdata) {
            imagedata = imgdata;
            buf = new Buffer(imgdata, 'binary');
            request.post({url : thisChannelResource, headers : {"Content-Type" : "image/jpeg"}, body : buf}, function (err, response, body) {
                error = err;
                resultObj = utils.parseJson(response, testName);
                returnedHref = resultObj['_links']['channel']['href'];
                var valueUrl = resultObj['_links']['self']['href'];
                utils.download(valueUrl, function (data) {
                    hubdata = data;
                    comparisonHasBeenFetched = true;
                });
            });
        });
    });

    waitsFor(function () {
        return comparisonHasBeenFetched;
    }, 5000);

    runs(function () {
        expect(error).toBeNull();
        expect(resultObj['_links']['channel']['href']).toBe(thisChannelResource);
        expect(hubdata.length).toEqual(imagedata.length);
        expect(hubdata).toEqual(imagedata);
    });
});
