/**
 * Created with JetBrains WebStorm.
 * User: gnewcomb
 * Date: 3/1/13
 * Time: 9:47 AM
 * To change this template use File | Settings | File Templates.
 */

// CREATE CHANNEL tests

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



describe('Create Channel:', function(){

    before(function(myCallback){
        channelName = dhh.makeRandomChannelName();
        agent = superagent.agent();
        dhh.makeChannel(channelName, function(res){
            if ((res.error) || (res.status != dhh.CHANNEL_CREATION_SUCCESS)) {
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

    // 404 trying to GET channel before it exists
    it('should return a 404 trying to GET channel before it exists', function(done){
        var myChannel = dhh.makeRandomChannelName();
        dhh.getChannel(myChannel, function(res) {
            expect(res.status).to.equal(404)
            done();
        });
    });


    it('Cannot create channel with blank name: 500 response', function(done){
        dhh.makeChannel('', function(res) {
            expect(res.status).to.equal(500);
            done();
        });

    });

    it('Cannot create channel with no/empty payload: 500 response', function(done) {
        agent.post(URL_ROOT +'/channel')
            .set('Content-Type', 'application/json')
            .send('')
            .end(function(err, res) {
                expect(res.status).to.equal(500);
                done();
            });
    });


    it('should return a 200 trying to GET channel after creation', function(done){
        dhh.getChannel(channelName, function(res) {
            expect(res.status).to.equal(200);
            done();
        });
    });

    // https://www.pivotaltracker.com/story/show/44113267
    // Attempting to create a channel with a name already in use will return an error. NOT IMPLEMENTED YET.
    it.skip('HTTP 500 if attempting to create channel with a name already in use', function(done) {
        dhh.makeChannel(channelName, function(res) {
            expect(res.status).to.equal(500);
            done();
        });
    });

});