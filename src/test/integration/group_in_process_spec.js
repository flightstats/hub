require('./integration_config.js');

var request = require('request');
var http = require('http');
var channelName = utils.randomChannelName();
var groupName = utils.randomChannelName();
var channelResource = channelUrl + "/" + channelName;
var testName = __filename;
var port = callbackPort + 5;
var callbackUrl = callbackDomain + ':' + port + '/';
var groupConfig = {
    callbackUrl : callbackUrl,
    channelUrl : channelResource,
    parallelCalls : 2
};

/**
 * This should:
 *
 * 1 - create a channel
 * 2 - create a group on that channel
 * 3 - post items into the channel
 * 4 - check status for inProcess
 */
describe(testName, function () {
    utils.createChannel(channelName);

    utils.putGroup(groupName, groupConfig);

    it('posts items and checks for in process ' + groupName, function (done) {

        for (var i = 0; i < 4; i++) {
            utils.postItem(channelResource);
        }

        setTimeout(function () {
            request.get({url : groupUrl,
                    headers : {"Content-Type" : "application/json"} },
                function (err, response, body) {
                    expect(err).toBeNull();
                    var parse = JSON.parse(body);
                    var status = parse.status;
                    var found = false;
                    status.forEach(function (stat) {
                       if (stat.name === groupName) {
                           expect(stat.inProcess).toContain(1000);
                           expect(stat.inProcess).toContain(1001);
                           found = true;
                       }
                    });
                    expect(found).toEqual(true);
                    done();
                });
        }, 2000);


    });

});

