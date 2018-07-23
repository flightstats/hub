require('../integration_config');
const {
    createChannel,
    fromObjectPath,
    getProp,
} = require('../lib/helpers');

var EventSource = require('eventsource');
var channelName = utils.randomChannelName();
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
    let createdChannel = false;

    beforeAll(async () => {
        const channel = await createChannel(channelName, false, testName);
        if (getProp('status', channel) === 201) {
            console.log(`created channel for ${testName}`);
            createdChannel = true;
        }
    });

    it('creates event source', function () {
        if (!createdChannel) return fail('channel not created in before block');
        var source = new EventSource(channelResource + '/events',
            {headers: {'Accept-Encoding': 'gzip'}});

        source.addEventListener('application/json', function (e) {
            console.log('message', e);
            events.push(getProp('lastEventId', e));
        }, false);

        source.addEventListener('open', function (e) {
            console.log('opened');
        }, false);
    });

    utils.itSleeps(1000);

    it('posts items', function (done) {
        if (!createdChannel) return done.fail('channel not created in before block');
        utils.postItemQ(channelResource)
            .then(function (value) {
                addPostedItem(value);
                return utils.postItemQ(channelResource);
            })
            .then(function (value) {
                addPostedItem(value);
                return utils.postItemQ(channelResource);
            })
            .then(function (value) {
                addPostedItem(value);
                return utils.postItemQ(channelResource);
            })
            .then(function (value) {
                addPostedItem(value);
                done();
            });

        function addPostedItem(value) {
            const selfLink = fromObjectPath(['body', '_links', 'self', 'href'], value);
            console.log('posted ', selfLink);
            postedItems.push(selfLink);
        }

    }, 10 * 1000);

    it('waits for data', function (done) {
        if (!createdChannel) return done.fail('channel not created in before block');
        utils.waitForData(postedItems, events, done);
    });

    it('verifies events', function () {
        console.log('events:', events);
        expect(postedItems.length).toBeGreaterThan(0);
        for (var i = 0; i < postedItems.length; i++) {
            expect(postedItems[i]).toBe(events[i]);
        }

    });

});
