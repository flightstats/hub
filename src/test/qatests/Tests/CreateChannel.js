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
var gu = require('../genericUtils.js');


var URL_ROOT = dhh.URL_ROOT,
    DEBUG = true;

// Test variables that are regularly overwritten
var agent, payload, req, uri, contentType, channelName;


describe('Create Channel: ', function(){

    before(function(myCallback){
        channelName = dhh.getRandomChannelName();
        agent = superagent.agent();
        dhh.createChannel(channelName, function(res){
            if ((res.error) || (!gu.isHTTPSuccess(res.status))) {
                //console.log('bad things');
                throw new Error(res.error);
            }
            console.log('Main test channel:'+ channelName);
            myCallback();
        });
    });

    beforeEach(function(){
        agent = superagent.agent();
        payload = uri = req = contentType = '';
    });

    it.skip('Create channel with name reserved by Cassandra', function(done) {
        channelName = 'channelMetadata';
        agent = superagent.agent();
        dhh.createChannel(channelName, function(res){
            if ((res.error) || (!gu.isHTTPSuccess(res.status))) {
                //console.log('bad things');
                throw new Error(res.error);
            }
            console.log('Main test channel:'+ channelName);
            done();
        });
    })

    // See:  https://www.pivotaltracker.com/story/show/49566971
    it('blank name not allowed', function(done){
        dhh.createChannel('', function(res) {
            expect(res.status).to.equal(gu.HTTPresponses.Bad_Request);
            gu.debugLog('Response status: '+ res.status, DEBUG);
            done();
        });

    });

    it('no / empty payload not allowed', function(done) {
        agent.post(URL_ROOT +'/channel')
            .set('Content-Type', 'application/json')
            .send('')
            .end(function(err, res) {
                expect(gu.isHTTPError(res.status)).to.equal(true);
                gu.debugLog('Response status: '+ res.status, DEBUG);
                done();
            });
    });


    it('(Acceptance) channel created', function(done){
        dhh.getChannel({'name': channelName}, function(res) {
            expect(gu.isHTTPSuccess(res.status)).to.equal(true);
            done();
        });
    });

    // https://www.pivotaltracker.com/story/show/44113267
    // Attempting to create a channel with a name already in use will return an error. NOT IMPLEMENTED YET.
    it.skip('<CODE NOT IMPLEMENTED> Error if attempting to create channel with a name already in use', function(done) {
        dhh.createChannel(channelName, function(res) {
            expect(gu.isHTTPError(res.status)).to.equal(true);
            done();
        });
    });

});