require('./integration_config.js');
var request = require('request');
var testName = __filename;
var channelName = utils.randomChannelName();

/**
 * 1 - Create local channel with replicationSource to non-existent channel
 * 2 - Delete local channel
 */
describe(testName, function () {
    var channelName = utils.randomChannelName();
    utils.putChannel(channelName, function () {
    }, {'replicationSource': 'http://hub/channel/none'});

    var localChannelUrl = hubUrlBase + '/channel/' + channelName;

    utils.itSleeps(1000);

    it('tries to add item to channel ' + channelName, function (done) {
        request.post({
                url: localChannelUrl,
                headers: {"Content-Type": "application/json", user: 'somebody'},
                body: JSON.stringify({"data": Date.now()})
            },
            function (err, response, body) {
                expect(err).toBeNull();

                console.log('body', body);

                expect(response.headers['content-type']).toBe('application/json');
                expect(response.statusCode).toBe(403);
                expect(body).toBe(channelName + ' cannot modified while replicating');
                done();
            });
    });

    it('tries to delete channel', function (done) {
        request.del({url : localChannelUrl },
            function (err, response, body) {
                expect(err).toBeNull();
                expect(response.statusCode).toBe(202);
                done();
            });
    });


});
