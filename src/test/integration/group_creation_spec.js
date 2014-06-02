require('./integration_config.js');

var request = require('request');
var http = require('http');
var groupName = utils.randomChannelName();
var groupResource = groupUrl + "/" + groupName;
var testName = __filename;
var groupConfig = {
    callbackUrl : 'http://nothing/callback',
    channelUrl: 'http://nothing/channel/notHere',
    transactional: false
};

/**
 * This should:
 *
 * 1 - create a group 
 * 2 - get the group
 * 3 - update the group
 * 4 - get the group
 * 5 - delete the group
 */
describe(testName, function () {

    utils.putGroup(groupName, groupConfig);

    it('gets the group ' + groupName, function (done) {
        request.get({url: groupResource,
                headers: {"Content-Type": "application/json"} },
            function (err, response, body) {
                expect(err).toBeNull();
                expect(response.statusCode).toBe(200);
                var parse = JSON.parse(body);
                expect(parse.callbackUrl).toBe(groupConfig.callbackUrl);
                expect(parse.channelUrl).toBe(groupConfig.channelUrl);
                expect(parse.transactional).toBe(groupConfig.transactional);
                expect(parse.name).toBe(groupName);
                done();
            });
    });

    it('deletes the group ' + groupName, function (done) {
        request.del({url: groupResource },
            function (err, response, body) {
                expect(err).toBeNull();
                expect(response.statusCode).toBe(202);
                done();
            });
    });

});

