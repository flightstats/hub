require('./integration_config.js');

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
 * PUT channel to same mutableTime - 200
 * add a now item - A
 * add a historical item - B
 * add an older historical - C
 *
 * change mutableTime to between B and C
 * verify B & A are returned for IMMUTABLE
 * verify C for Mutable
 * all for ALL
 */
describe(testName, function () {


});
