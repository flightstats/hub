require('./integration_config.js');

var request = require('request');
var EventSource = require('eventsource');
var http = require('http');
var channelName = utils.randomChannelName();
var groupName = utils.randomChannelName();
var channelResource = channelUrl + "/" + channelName;
var testName = __filename;

/**
 * This should:
 *
 * 1 - create a channel
 * 2 - start the events events on that channel
 * 3 - post items to the channel
 * 4 - verify that the events are returned within delta time
 */
describe(testName, function () {
    var events = [];
    var postedItems = [];

    utils.createChannel(channelName, false, testName);

    it('creates event source', function () {

        var source = new EventSource(channelResource + '/events',
            {headers: {'Accept-Encoding': 'gzip'}});

        source.addEventListener('application/json', function (e) {
            console.log('message', e);
            events.push(e.lastEventId);
        }, false);

        source.addEventListener('open', function (e) {
            console.log('opened');
        }, false);


        utils.postItemQ(channelResource)
            .then(function (value) {
                return postedItem(value, true);
            }).then(function (value) {
                return postedItem(value, true);
            }).then(function (value) {
                return postedItem(value, true);
            }).then(function (value) {
                postedItem(value, false);
            });

        waitsFor(function () {
            return events.length == 4;
        }, 9999);

        function postedItem(value, post) {
            console.log('posted ', value.body._links.self.href);
            postedItems.push(value.body._links.self.href);
            if (post) {
                return utils.postItemQ(channelResource);
            }
        }

    }, 1000);

    it('verifies events', function () {
        for (i = 0; i < postedItems.length; i++) {
            expect(postedItems[i]).toBe(events[i]);
        }

    })

});

