/**
 * Created with IntelliJ IDEA.
 * User: gnewcomb
 * Date: 6/26/13
 * Time: 3:25 PM
 * To change this template use File | Settings | File Templates.
 */

var chai = require('chai'),
    expect = chai.expect,
    superagent = require('superagent'),
    request = require('request'),
    moment = require('moment'),
    async = require('async'),
    lodash = require('lodash');

var dhh = require('.././DH_test_helpers/DHtesthelpers.js'),
    gu = require('../genericUtils.js'),
    ranU = require('../randomUtils.js');

var DEBUG = true;

describe.skip('CODE NOT YET IMPLEMENTED - Patch Channel', function() {

    var mainChannel = {
        name: null,
        ttl: null
    }

    before(function(callback){
        mainChannel.name = dhh.getRandomChannelName();

        dhh.createChannel({name: channelName}, function(res){
            if ((res.error) || (!gu.isHTTPSuccess(res.status))) {
                throw new Error(res.error);
            }
            console.log('Main test channel:'+ channelName);
            mainChannel.ttl = res.body.ttl;

            callback();
        });
    });

    describe('Acceptance', function() {

    })
})

