require('../integration_config');

var channel = utils.randomChannelName();
var moment = require('moment');

var testName = __filename;
/**
 * This should:
 * Create a historical channel
 * Add an item
 * Add an item before the first
 */
describe(testName, function () {

    var mutableTime = moment.utc().subtract(1, 'minute');

    var channelBody = {
        mutableTime: mutableTime.format('YYYY-MM-DDTHH:mm:ss.SSS'),
        tags: ["test"]
    };

    utils.putChannel(channel, false, channelBody, testName);

    var channelUrl = hubUrlBase + '/channel/' + channel + '/';
    utils.addItem(channelUrl + '2016/06/01/12/00/00/000', 201);
    utils.addItem(channelUrl + '2016/06/01/11/00/00/000', 201);

});
