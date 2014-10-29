require('./../integration/integration_config.js');

var request = require('request');
var channelName = utils.randomChannelName();
var channelResource = channelUrl + "/" + channelName;
var testName = __filename;

/**
 * This test is only useful in a distributed environment.
 *
 * create a channel
 * add some items
 * delete the channel
 * wait for 1s
 * re-create the same channel
 * add an item
 * validate that the sequence restarted at 1000
 *
 */
describe(testName + ' ' + channelName, function () {
    utils.createChannel(channelName);

    utils.addItem(channelResource);
    utils.addItem(channelResource);
    utils.addItem(channelResource);

    it("deletes channel", function (done) {
        request.del({url : channelResource},
            function (err, response, body) {
                expect(err).toBeNull();
                expect(response.statusCode).toBe(202);
                done();
            });

    });

    utils.sleep(1000);

    utils.createChannel(channelName);
    utils.addItem(channelResource);

    //todo - gfm - 5/23/14 - woud be great to check all instances in a cluster.
    it("gets latest " + channelResource, function (done) {
        request.get({url : channelResource + '/latest'},
            function (err, response, body) {
                expect(err).toBeNull();
                expect(response.statusCode).toBe(200);
                expect(response.request.href).toBe(channelResource + '/1000');
                done();
            });
    })

});
