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

    var sleep_time = 20 * 1000;

    var sourceName = utils.randomChannelName();
    var remoteChannelUrl = 'http://' + hubDomain + '/channel';
    utils.createChannel(sourceName, remoteChannelUrl);

    var replicationSource = remoteChannelUrl + '/' + sourceName;
    console.log('replicationSource', replicationSource);

    var destinationName = utils.randomChannelName();
    utils.putChannel(destinationName, function () {
    }, {'replicationSource': replicationSource, 'ttlDays': 1});

    var replicatedUrl = hubUrlBase + '/channel/' + destinationName;
    console.log('replicatedUrl', replicatedUrl);

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
                    done();
                })
                .catch(function (error) {
                    console.log('error', error);
                })
        });
    }

    postTwoItems(2);


    utils.sleep(sleep_time);

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

    }, sleep_time);

    utils.putChannel(destinationName, function () {
    }, {'replicationSource': '', 'ttlDays': 1});

    utils.sleep(sleep_time);

    postTwoItems(4);

    utils.putChannel(destinationName, function () {
    }, {'replicationSource': replicationSource, 'ttlDays': 1});

    postTwoItems(6);

    utils.sleep(sleep_time);

    it('verfies new items are in local channel ' + secondItemUrl, function (done) {
        request.get({
                url: secondItemUrl + '/next/10?stable=false',
                headers: {"Content-Type": "application/json"}
            },
            function (err, response, body) {
                expect(err).toBeNull();
                expect(response.statusCode).toBe(200);
                var parse = JSON.parse(body);
                console.log('next uris ', parse._links.uris);
                expect(parse._links.uris.length).toBe(4);
                done();
            });

    }, sleep_time);


});
