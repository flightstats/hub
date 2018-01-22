require('../integration_config');
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
    console.log('v1 replicationSource', replicationSource);

    var replicatedName = utils.randomChannelName();
    utils.putChannel(replicatedName, function () {
    }, {'replicationSource': replicationSource, 'ttlDays': 1});

    var replicatedUrl = hubUrlBase + '/channel/' + replicatedName;
    console.log('v1 replicatedUrl', replicatedUrl);

    var items = [];

    function postTwoItems(expected) {
        it('posts items ' + replicationSource, function (done) {
            utils.postItemQ(replicationSource)
                .then(function (value) {
                    items.push(value.body._links.self.href);
                    return utils.postItemQ(replicationSource);
                })
                .then(function (value) {
                    items.push(value.body._links.self.href);
                    expect(items.length).toBe(expected);
                })
                .finally(done);
        });
    }

    postTwoItems(2);

    utils.sleep(10 * 1000);

    var secondItemUrl = '';

    function getSequence(uri) {
        return parseInt(uri.substring(uri.lastIndexOf('/') + 1));
    }

    it('verfies items are in local channel', function (done) {
        request.get({
                url: replicatedUrl + '/status?stable=false',
                headers: {"Content-Type": "application/json"}
            },
            function (err, response, body) {
                expect(err).toBeNull();
                expect(response.statusCode).toBe(200);
                var parse = JSON.parse(body);
                expect(getSequence(parse._links.latest.href)).toBe(1001);
                secondItemUrl = parse._links.latest.href;
                done();
            });

    }, 10 * 1000);

    utils.putChannel(replicatedName, function () {
    }, {'replicationSource': '', 'ttlDays': 1});

    utils.sleep(10 * 1000);

    postTwoItems(4);

    utils.putChannel(replicatedName, function () {
    }, {'replicationSource': replicationSource, 'ttlDays': 1});

    postTwoItems(6);

    utils.sleep(10 * 1000);

    it('verfies new items are in local channel ' + secondItemUrl, function (done) {
        console.log('calling next/10 on', secondItemUrl);
        /*request.get({
                url: secondItemUrl + '/next/10?stable=false',
                headers: {"Content-Type": "application/json"}
            },
            function (err, response, body) {
                expect(err).toBeNull();
         if (!response) {
         expect(response).not.toBeUndefined();
         done();
         return;
         }
                expect(response.statusCode).toBe(200);
                var parse = JSON.parse(body);
                console.log('next uris ', parse._links.uris);
                expect(parse._links.uris.length).toBe(4);
                done();
         });*/

        done();
    }, 10 * 1000);


});
