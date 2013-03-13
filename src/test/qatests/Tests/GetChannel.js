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
var testRandom = require('../js_testing_utils/randomUtils.js');


var URL_ROOT = dhh.URL_ROOT;

// Test variables that are regularly overwritten
var agent
    , payload
    , fileAsAStream
    , req
    , uri
    , contentType;

var channelName;


describe('GET Channel metadata:', function() {

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

    it('Contains expected metadata', function(done) {
        var cnMetadata;
        var thisChannel = dhh.makeRandomChannelName();

        dhh.makeChannel(thisChannel, function(makeRes) {
            expect(makeRes.status).to.equal(200);

            dhh.postData(thisChannel, testRandom.randomString(testRandom.randomNum(51)), function(postRes, myUri) {
                expect(postRes.status).to.equal(200);

                dhh.getChannel(thisChannel, function(cnRes) {
                    expect(cnRes.status).to.equal(200);

                    cnMetadata = new dhh.channelMetadata(cnRes.body);
                    expect(cnMetadata.getChannelUri()).to.not.be.null;
                    expect(moment(cnMetadata.getCreationDate()).isValid()).to.be.true;
                    expect(cnMetadata.getLatestUri()).to.not.be.null;
                    expect(cnMetadata.getName()).to.equal(thisChannel);

                    done();
                })
            });
        });

    });
});
