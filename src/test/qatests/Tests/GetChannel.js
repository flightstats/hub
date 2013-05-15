/**
 * Created with JetBrains WebStorm.
 * User: gnewcomb
 * Date: 3/1/13
 * Time: 9:47 AM
 * To change this template use File | Settings | File Templates.
 */
var chai = require('chai');
var expect = chai.expect;
var superagent = require('superagent');
var request = require('request');
var moment = require('moment');
var async = require('async');
var fs = require('fs');


var dhh = require('../DH_test_helpers/DHtesthelpers.js');
var ranU = require('../randomUtils.js');
var gu = require('../genericUtils.js');


var URL_ROOT = dhh.URL_ROOT;

// Test variables that are regularly overwritten
var agent
    , payload
    , uri;

var channelName;


describe('GET Channel metadata:', function() {

    before(function(myCallback){
        channelName = dhh.getRandomChannelName();
        agent = superagent.agent();
        dhh.createChannel(channelName, function(res){
            if ((res.error) || (!gu.isHTTPSuccess(res.status))) {
                myCallback(res.error);
            };
            gu.debugLog('Main test channel:'+ channelName);
            myCallback();
        });
    });

    beforeEach(function(){
        payload = uri = '';
    })

    it('metadata content is present', function(done) {
        var channelName = dhh.getRandomChannelName();

        dhh.createChannel(channelName, function(makeRes, channelUri) {
            expect(gu.isHTTPSuccess(makeRes.status)).to.equal(true);

            dhh.postData(channelUri, ranU.randomString(ranU.randomNum(51)), function(postRes, myUri) {
                expect(gu.isHTTPSuccess(postRes.status)).to.equal(true);

                dhh.getChannel({'uri': channelUri} , function(cnRes) {
                    expect(gu.isHTTPSuccess(cnRes.status)).to.equal(true);

                    var cnMetadata = new dhh.channelMetadata(cnRes.body);
                    expect(cnMetadata.getChannelUri()).to.not.be.null;
                    expect(moment(cnMetadata.getCreationDate()).isValid()).to.be.true;
                    expect(cnMetadata.getLatestUri()).to.not.be.null;
                    expect(cnMetadata.getWebSocketUri()).to.not.be.null;
                    expect(cnMetadata.getName()).to.equal(channelName);

                    done();
                });
            });
        });

    });

    // 404 trying to GET channel before it exists
    it('should return a 404 trying to GET channel before it exists', function(done){
        var myChannel = dhh.getRandomChannelName();
        dhh.getChannel({'name': myChannel}, function(res) {
            expect(gu.isHTTPError(res.status)).to.equal(true);
            done();
        });
    });
});
