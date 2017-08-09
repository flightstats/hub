require('../integration_config');

var request = require('request');
var http = require('http');
var parse = require('parse-link-header');
var channel = utils.randomChannelName();
var moment = require('moment');

var testName = __filename;
var channelBody = {};
/**
 * This should:
 * Create a normal channel
 * Verify no items can be inserted through the historical interface
 */
describe(testName, function () {

    utils.putChannel(channel, false, channelBody, testName);
    var channelUrl = hubUrlBase + '/channel/' + channel + '/';
    utils.addItem(channelUrl + '2016/06/01/12/00/00/000', 403);
    utils.addItem(channelUrl + moment.utc().format('YYYY/MM/DD/HH/mm/ss/SSS'), 403);

    var mutableTime = moment.utc().subtract(1, 'minute');
    utils.putChannel(channel, false, {mutableTime: mutableTime.format('YYYY-MM-DDTHH:mm:ss.SSS')}, testName, 400);

});
