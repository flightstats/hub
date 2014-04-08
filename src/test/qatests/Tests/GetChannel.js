/**
 * Created with JetBrains WebStorm.
 * User: gnewcomb
 * Date: 3/1/13
 * Time: 9:47 AM
 * To change this template use File | Settings | File Templates.
 */
var chai = require('chai'),
    expect = chai.expect,
    superagent = require('superagent'),
    request = require('request'),
    moment = require('moment'),
    async = require('async'),
    lodash = require('lodash');


var dhh = require('../DH_test_helpers/DHtesthelpers.js'),
    ranU = require('../randomUtils.js'),
    gu = require('../genericUtils.js');


describe('GET Channel metadata:', function() {

    describe('Acceptance', function() {
        var channelName,
            getCnResponse,
            cnUri,
            cnMetadata;

        before(function(done){
            channelName = dhh.getRandomChannelName();

            dhh.createChannel({name: channelName, debug: true}, function(makeRes, channelUri) {
                expect(gu.isHTTPSuccess(makeRes.status)).to.equal(true);
                cnUri = channelUri;

                dhh.postData({channelUri: channelUri, data: ranU.randomString(ranU.randomNum(51)), debug: true}, function(postRes, myUri) {
                    expect(gu.isHTTPSuccess(postRes.status)).to.equal(true);

                    dhh.getChannel({'uri': channelUri} , function(cnRes) {

                        getCnResponse = cnRes;
                        cnMetadata = new dhh.channelMetadata(getCnResponse.body);

                        done();

                    });
                });
            });
        });

        it('returns a 200 on success', function() {
            expect(getCnResponse.status).to.equal(gu.HTTPresponses.OK);
        })

        it('metadata structure is correct', function() {
            var body = getCnResponse.body;

//            expect(body.hasOwnProperty('_links')).to.be.true;
//            expect(body._links.hasOwnProperty('self')).to.be.true;
//            expect(body._links.self.hasOwnProperty('href')).to.be.true;
//            expect(body._links.hasOwnProperty('latest')).to.be.true;
//            expect(body._links.latest.hasOwnProperty('href')).to.be.true;
//            expect(body._links.hasOwnProperty('ws')).to.be.true;
//            expect(body._links.ws.hasOwnProperty('href')).to.be.true;
//            expect(body.hasOwnProperty('lastUpdateDate')).to.be.true;
//            expect(body.hasOwnProperty('name')).to.be.true;
//            expect(body.hasOwnProperty('creationDate')).to.be.true;
//            expect(body.hasOwnProperty('ttlMillis')).to.be.true;
//
//            expect(lodash.keys(body).length).to.equal(5);
//            expect(lodash.keys(body._links).length).to.equal(3);
//

            expect(body.hasOwnProperty('_links')).to.be.true;
            expect(body._links.hasOwnProperty('self')).to.be.true;
            expect(body._links.self.hasOwnProperty('href')).to.be.true;
            expect(body._links.hasOwnProperty('latest')).to.be.true;
            expect(body._links.latest.hasOwnProperty('href')).to.be.true;
            expect(body._links.hasOwnProperty('ws')).to.be.true;
            expect(body._links.ws.hasOwnProperty('href')).to.be.true;
            expect(body.hasOwnProperty('name')).to.be.true;
            expect(body.hasOwnProperty('creationDate')).to.be.true;
            expect(body.hasOwnProperty('ttlDays')).to.be.true;
            expect(body.hasOwnProperty('type')).to.be.true;
            expect(body.hasOwnProperty('contentSizeKB')).to.be.true;
            expect(body.hasOwnProperty('peakRequestRateSeconds')).to.be.true;
            expect(body.hasOwnProperty('ttlMillis')).to.be.true;

            expect(lodash.keys(body).length).to.equal(8);
            expect(lodash.keys(body._links).length).to.equal(4);
        });

        it('returns correct channel uri', function() {
            expect(cnMetadata.getChannelUri()).to.equal(cnUri);
        })

        it('returns correct creationDate', function() {
            var returnedCreationDate = moment(cnMetadata.getCreationDate());

            expect(returnedCreationDate.add('minutes', 5).isAfter(moment())).to.be.true;
        })

        it.skip('lastUpdateDate NO LONGER GIVEN - returns correct lastUpdateDate', function() {
            var returnedCreationDate = moment(cnMetadata.getCreationDate()),
                returnedLastUpdateDate = moment(cnMetadata.getLastUpdateDate());

            expect(returnedLastUpdateDate.isBefore(returnedCreationDate)).to.be.false;
            expect(returnedLastUpdateDate.add('minutes', 5).isAfter(moment())).to.be.true;
        })

        it('returns correct name', function() {
            expect(cnMetadata.getName()).to.equal(channelName);
        })

        it('returns valid TTL', function() {
            expect(lodash.isNumber(cnMetadata.getTTL())).to.be.true;
        })

        it('should successfully GET real channel but with trailing slash', function(done){
            dhh.getChannel({uri: cnUri +'/' }, function(res) {
                expect(res.status).to.equal(gu.HTTPresponses.OK);
                done();
            });
        });
    })

    describe('Error cases', function() {

        it('should return a 404 trying to GET channel that does not exist', function(done){
            dhh.getChannel({'name': dhh.getRandomChannelName()}, function(res) {
                expect(gu.isHTTPError(res.status)).to.equal(true);
                done();
            });
        });
    })


});
