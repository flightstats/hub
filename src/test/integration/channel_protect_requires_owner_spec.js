require('./integration_config.js');

var request = require('request');

describe(__filename, function () {

    /**
     * When creating a new channel and protect is true,
     * the owner field must also be provided.
     */

    describe('new channel', function () {
        var channelName = utils.randomChannelName();
        utils.putChannel(channelName, false, {protect: true}, 'protected channel without owner set', 400);
        utils.putChannel(channelName, false, {protect: true, owner: 'someone'}, 'protected channel with owner set', 201);
    });

    /**
     * When updating an existing channel to be protected,
     * the owner field must already exist if not provided.
     */

    describe('update channel with existing owner', function () {
        var channelName = utils.randomChannelName();
        utils.putChannel(channelName, false, {}, 'default channel', 201);
        utils.putChannel(channelName, false, {protect: true}, 'protect channel', 400);
        utils.putChannel(channelName, false, {owner: 'someone'}, 'set owner', 201);
        utils.putChannel(channelName, false, {protect: true}, 'protect channel', 201);
    });

    /**
     * When updating an existing channel to be protected,
     * the owner field must be provided if it doesn't already exist.
     */

    describe('update channel without existing owner', function () {
        var channelName = utils.randomChannelName();
        utils.putChannel(channelName, false, {}, 'default channel', 201);
        utils.putChannel(channelName, false, {protect: true}, 'protect channel', 400);
        utils.putChannel(channelName, false, {protect: true, owner: 'someone'}, 'protect channel and set owner', 201);
    });

});
