require('./integration_config.js');
var request = require('request');
var testName = __filename;
var channelName = utils.randomChannelName();

/**
 * 1 - Create remote channel
 * 2 - Create local channel with replicationSource to remote channel
 * 3 - add items to remote channel
 * 4 - verify items are replicated locally
 * 5 - delete local channel
 * 6 - add items to remote channel
 * 7 - recreate local channel
 * 8 - add items to remote channel
 * 9 - verify items are replicated locally
 */
describe(testName, function () {

    var sourceName = utils.randomChannelName();
    var remoteChannelUrl = 'http://' + replicationDomain + '/channel';
    utils.createChannel(sourceName, remoteChannelUrl);

    var replicationSource = remoteChannelUrl + '/' + sourceName;
    console.log('replicationSource', replicationSource);

    var replicatedName = utils.randomChannelName();
    utils.putChannel(replicatedName, function () {
    }, {'replicationSource': replicationSource, 'ttlDays': 1});

    var replicatedUrl = hubUrlBase + '/channel/' + replicatedName;
    console.log('replicatedUrl', replicatedUrl);

    var items = [];

    it('posts items ' + replicationSource, function (done) {
        utils.postItemQ(replicationSource)
            .then(function (value) {
                items.push(value.body._links.self.href);
                return utils.postItemQ(replicationSource);
            })
            .then(function (value) {
                items.push(value.body._links.self.href);
                expect(items.length).toBe(2);
                done();
            })
            .catch(function (error) {
                console.log('error', error);
            })
    });

    utils.sleep(10 * 1000);

    function getSequence(uri) {
        return parseInt(uri.substring(uri.lastIndexOf('/') + 1));
    }

    it('verfies items are in local channel', function (done) {
        var url = replicatedUrl + '/status';
        console.log('calling', url);
        request.get({
                url: url,
                headers: {"Content-Type": "application/json"}
            },
            function (err, response, body) {
                expect(err).toBeNull();
                expect(response.statusCode).toBe(200);
                var parse = JSON.parse(body);
                expect(getSequence(parse._links.latest.href)).toBe(1001);
                done();
            });

    }, 10 * 1000);

    //utils.sleep(1000);


    /*it('tries to delete channel', function (done) {
     request.del({url : localChannelUrl },
     function (err, response, body) {
     expect(err).toBeNull();
     expect(response.statusCode).toBe(202);
     done();
     });
     });*/


});
