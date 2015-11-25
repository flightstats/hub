require('./integration_config.js');

var request = require('request');
var http = require('http');
var groupName1 = utils.randomChannelName();
var groupName2 = utils.randomChannelName();
var groupResource = groupUrl + "/" + groupName1;
var testName = __filename;
var groupConfig = {
    callbackUrl : 'http://nothing/callback',
    channelUrl : 'http://nothing/channel/notHere'
};

/**
 * This should:
 *
 * 1 - create groups
 * 2 - make sure they exist
 */
describe(testName, function () {

    var groupHrefs = [
        utils.putGroup(groupName1, groupConfig, 201, testName),
        utils.putGroup(groupName2, groupConfig, 201, testName)
    ];
    var foundGroupHrefs = [];

    it('gets the groups ', function () {
        runs(function () {
            request.get({url : groupUrl, headers : {'Content-Type' : 'application/json'} },
                function (err, response, body) {
                    expect(err).toBeNull();
                    expect(response.statusCode).toBe(200);
                    var parse = utils.parseJson(response, testName);
                    expect(parse._links.self.href).toBe(groupUrl);
                    var groups = parse._links.groups;
                    groups.forEach(function (item) {
                        if (item.name === groupName1 || item.name === groupName2) {
                            foundGroupHrefs.push(item.href);
                        }
                    });
                });
        });

        waitsFor(function () {
            return foundGroupHrefs.length === 2;
        });

        runs(function () {
            groupHrefs.forEach(function (item) {
                expect(foundGroupHrefs.indexOf(item)).not.toBe(-1);
            })
        });

    });

    utils.deleteGroup(groupName1);
    utils.deleteGroup(groupName2);

});

