require('./integration_config.js');

var request = require('request');
var http = require('http');
var groupName = utils.randomChannelName();
var groupResource = utils.getGroupUrl() + "/" + groupName;
var testName = __filename;

describe(testName, function () {

    it('gets missing group ' + groupName, function (done) {
        request.get({url : groupResource,
                headers : {"Content-Type" : "application/json"} },
            function (err, response, body) {
                expect(err).toBeNull();
                expect(response.statusCode).toBe(404);
                done();
            });
    });

});

