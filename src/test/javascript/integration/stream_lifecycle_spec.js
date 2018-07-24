require('../integration_config');
const { createChannel, getProp } = require('../lib/helpers');
var channelName = utils.randomChannelName();
var channelResource = channelUrl + "/" + channelName;
var testName = __filename;

/**
 * This should:
 *
 * 1 - create a channel
 * 2 - create a stream on that channel
 * 3 - post items into the channel
 * 4 - verify that the item payloads are returned within delta time
 */

xdescribe(testName, function () {
    var callbackItems = [];
    var postedItems = [];
    let createdChannel = false;

    beforeAll(async () => {
        const channel = await createChannel(channelName, false, testName);
        if (getProp('statusCode', channel) === 201) {
            createdChannel = true;
            console.log(`created channel for ${__filename}`);
        }
    });

    it('opens a stream', function (done) {
        if (!createdChannel) return done.fail('channel not created in before block');
        var url = channelResource + '/stream';
        var headers = {"Content-Type": "application/json"};

        utils.httpGet(url, headers)
            .then(function (response) {
                expect(getProp('statusCode', response)).toBe(200);
                // console.log('body', body);
            })
            .finally(done);
    });

    it('inserts multiple items', function (done) {
        if (!createdChannel) return done.fail('channel not created in before block');
        utils.postItemQ(channelResource)
            .then(function (value) {
                postedItems.push(value);
                return utils.postItemQ(channelResource);
            })
            .then(function (value) {
                postedItems.push(value);
                return utils.postItemQ(channelResource);
            })
            .then(function (value) {
                postedItems.push(value);
                return utils.postItemQ(channelResource);
            })
            .then(function (value) {
                postedItems.push(value);
                done();
            });
    });

    it('waits for the data', function (done) {
        if (!createdChannel) return done.fail('channel not created in before block');
        utils.waitForData(callbackItems, postedItems, done);
    });

    it('verifies we got the correct number of items', function (done) {
        if (!createdChannel) return done.fail('channel not created in before block');
        expect(callbackItems.length).toEqual(4);
    });
});
