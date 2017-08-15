require('../integration_config');

var request = require('request');
var http = require('http');
var parse = require('parse-link-header');
var channel = utils.randomChannelName();
var moment = require('moment');

var tag = Math.random().toString().replace(".", "");
var testName = __filename;


/**
 * This should:
 * Create a channel with mutableTime
 * insert a historical item after the mutableTime
 */
describe(testName, function () {

    var mutableTime = moment.utc().subtract(2, 'minute');

    var channelBody = {
        mutableTime: mutableTime.format('YYYY-MM-DDTHH:mm:ss'),
        tags: [tag, "test"]
    };

    utils.putChannel(channel, false, channelBody, testName);

    var channelURL = hubUrlBase + '/channel/' + channel;
    var pointInThePastURL = channelURL + '/' + mutableTime.add(1, 'minute').format('YYYY/MM/DD/HH/mm/ss/SSS');

    var historicalLocation;

    utils.addItem(pointInThePastURL, 400);

});
