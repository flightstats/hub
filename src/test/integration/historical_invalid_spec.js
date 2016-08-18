require('./integration_config.js');

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
    utils.putChannel(channel, false, {historical: true}, testName, 400);

});
