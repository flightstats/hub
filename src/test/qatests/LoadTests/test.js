/**
 * Created with JetBrains WebStorm.
 * User: gnewcomb
 * Date: 3/1/13
 * Time: 9:47 AM
 * To change this template use File | Settings | File Templates.
 */

// Load tests

var chai = require('chai');
var expect = chai.expect;
var superagent = require('superagent');
var crypto = require('crypto');
var request = require('request');
var moment = require('moment');
var async = require('async');
var fs = require('fs');

var dhh = require('.././DH_test_helpers/DHtesthelpers.js');
var testRandom = require('.././js_testing_utils/randomUtils.js');


var URL_ROOT = dhh.URL_ROOT;

// Test variables that are regularly overwritten
var agent
    , payload
    , fileAsAStream
    , req
    , uri
    , contentType;

var channelName;

before(function(myCallback){
    channelName = dhh.makeRandomChannelName();
    agent = superagent.agent();
    dhh.makeChannel(channelName, function(res){
        if ((res.error) || (res.status != 200)) {
            myCallback(res.error);
        };
        console.log('Main test channel:'+ channelName);
        myCallback();
    });
});

beforeEach(function(){
    agent = superagent.agent();
    payload = uri = req = contentType = '';
})


describe.skip('Load tests - POST data:', function(){

    var loadChannels = {};
    var loadChannelKeys = [];  // channel.uri (to fetch data) and channel.data, e.g. { con {uri: x, data: y}}

    // To ignore the Loadtest cases:  mocha -R nyan --timeout 4000 --grep Load --invert
    it('Loadtest - POST rapidly to ten different channels, then confirm data retrieved via GET is correct', function(done){
        var cnMetadata;

        for (var i = 1; i <= 10; i++)
        {
            var thisName = dhh.makeRandomChannelName();
            var thisPayload = testRandom.randomString(Math.round(Math.random() * 50));
            loadChannels[thisName] = {"uri":'', "data":thisPayload};

        }

        for (var x in loadChannels){
            if (loadChannels.hasOwnProperty(x)) {
                loadChannelKeys.push(x);
            }
        }

        async.each(loadChannelKeys, function(cn, callback) {
            dhh.makeChannel(cn, function(res) {
                expect(res.status).to.equal(200);
                cnMetadata = new dhh.channelMetadata(res.body);
                expect(cnMetadata.getChannelUri()).to.equal(URL_ROOT +'/channel/'+ cn);
                callback();
            });

        }, function(err) {
            if (err) {
                throw err;
            };

            async.each(loadChannelKeys, function(cn, callback) {

                dhh.postData(cn,loadChannels[cn].data, function(res, uri) {
                    loadChannels[cn].uri = uri;
                    callback();
                });

            }, function(err) {
                if (err) {
                    throw err;
                };

                async.eachSeries(loadChannelKeys, function(cn, callback) {
                    uri = loadChannels[cn].uri;
                    payload = loadChannels[cn].data;

                    dhh.getValidationString(uri, payload, function(){
                        //console.log('Confirmed data retrieval from channel: '+ cn);
                        callback();
                    });
                }, function(err) {
                    if (err) {
                        throw err;
                    };

                    done();
                });

            });

        });

    });
});