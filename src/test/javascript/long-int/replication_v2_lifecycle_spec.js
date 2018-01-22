require('../integration_config');
var request = require('request');
var testName = __filename;

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

    var sleep_time = 10 * 1000;

    var sourceName = utils.randomChannelName();
    var remoteChannelUrl = 'http://' + replicationV2Domain + '/channel';
    utils.createChannel(sourceName, remoteChannelUrl);

    var replicationSource = remoteChannelUrl + '/' + sourceName;
    console.log('v2 replicationSource', replicationSource);

    var destinationName = utils.randomChannelName();
    utils.putChannel(destinationName, function () {
    }, {'replicationSource': replicationSource, 'ttlDays': 1});

    var destinationUrl = hubUrlBase + '/channel/' + destinationName;
    console.log('v2 destinationUrl', destinationUrl);

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


    utils.sleep(sleep_time);

    var secondItemUrl = '';

    function getContentKey(uri, channel) {
        return uri.substring(uri.lastIndexOf(channel) + channel.length);
    }

    it('verfies items are in local channel', function (done) {
        request.get({
                url: destinationUrl + '/status?stable=false',
                headers: {"Content-Type": "application/json"}
            },
            function (err, response, body) {
                expect(err).toBeNull();
                expect(response.statusCode).toBe(200);
                var parse = JSON.parse(body);
                var latestKey = getContentKey(parse._links.latest.href, destinationName);
                var sourceKey = getContentKey(items[1], sourceName);
                expect(latestKey).toBe(sourceKey);
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
        console.log('calling next/10', secondItemUrl);
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
