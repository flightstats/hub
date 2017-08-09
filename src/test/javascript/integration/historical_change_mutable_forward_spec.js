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
 *
 * Attempt to move the mutableTime forward
 */
describe(testName, function () {

    utils.putChannel(channel, false, {
        mutableTime: moment.utc().subtract(1, 'day').format('YYYY-MM-DDTHH:mm:ss.SSS'),
        tags: ["test"]
    }, testName);

    utils.putChannel(channel, false, {
        mutableTime: moment.utc().subtract(1, 'hour').format('YYYY-MM-DDTHH:mm:ss.SSS'),
        tags: ["test"]
    }, testName, 400);


});
