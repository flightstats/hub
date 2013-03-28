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


var URL_ROOT = dhh.URL_ROOT;
var DEBUG = dhh.DEBUG;

// Test variables that are regularly overwritten
var agent, payload, req, uri, contentType, channelName;


describe('Create Channel: ', function(){

    before(function(myCallback){
        channelName = dhh.makeRandomChannelName();
        agent = superagent.agent();
        dhh.makeChannel(channelName, function(res){
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

    it('blank name not allowed', function(done){
        dhh.makeChannel('', function(res) {
            expect(gu.isHTTPError(res.status)).to.equal(true);
            done();
        });

    });

    it('no / empty payload not allowed', function(done) {
        agent.post(URL_ROOT +'/channel')
            .set('Content-Type', 'application/json')
            .send('')
            .end(function(err, res) {
                expect(gu.isHTTPError(res.status)).to.equal(true);
                done();
            });
    });


    it('(Acceptance) channel created', function(done){
        dhh.getChannel(channelName, function(res) {
            expect(gu.isHTTPSuccess(res.status)).to.equal(true);
            done();
        });
    });

    // https://www.pivotaltracker.com/story/show/44113267
    // Attempting to create a channel with a name already in use will return an error. NOT IMPLEMENTED YET.
    it.skip('Error if attempting to create channel with a name already in use', function(done) {
        dhh.makeChannel(channelName, function(res) {
            expect(gu.isHTTPError(res.status)).to.equal(true);
            done();
        });
    });

});