/**
 * Created with JetBrains WebStorm.
 * User: gnewcomb
 * Date: 3/1/13
 * Time: 9:47 AM
 * To change this template use File | Settings | File Templates.
 */

// CREATE CHANNEL tests

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


describe('Create Channel: ', function(){

    var channelName;

    before(function(myCallback){
        channelName = dhh.getRandomChannelName();
        dhh.createChannel(channelName, function(res){
            if ((res.error) || (!gu.isHTTPSuccess(res.status))) {
                //console.log('bad things');
                throw new Error(res.error);
            }
            console.log('Main test channel:'+ channelName);
            myCallback();
        });
    });

    describe('Acceptance', function() {

        var createRes,
            channelUri,
            acceptName;

        before(function(done) {

            acceptName = dhh.getRandomChannelName();

            dhh.createChannel(acceptName, function(res, uri) {
                createRes = res;
                channelUri = uri;

                done();
            });
        })

        it('channel creation returns 201 (Created)', function(){
            expect(createRes.status).to.equal(gu.HTTPresponses.Created);
        });

        it('channel uri is correct', function(done) {

            dhh.getChannel({'uri': channelUri}, function(res) {
                expect(res.status).to.equal(gu.HTTPresponses.OK);
                expect(res.body._links.self.href).to.equal(channelUri);

                done();
            })
        })

        it('creation response has correctly structured body', function() {
            var body = createRes.body;

            expect(body.hasOwnProperty('_links')).to.be.true;
            expect(body._links.hasOwnProperty('self')).to.be.true;
            expect(body._links.self.hasOwnProperty('href')).to.be.true;
            expect(body._links.hasOwnProperty('latest')).to.be.true;
            expect(body._links.latest.hasOwnProperty('href')).to.be.true;
            expect(body._links.hasOwnProperty('ws')).to.be.true;
            expect(body._links.ws.hasOwnProperty('href')).to.be.true;
            expect(body.hasOwnProperty('name')).to.be.true;
            expect(body.hasOwnProperty('creationDate')).to.be.true;

            expect(lodash.keys(body).length).to.equal(3);
            expect(lodash.keys(body._links).length).to.equal(3);

        })

        it('creation response includes correct Location header', function() {
            expect(createRes.headers.hasOwnProperty('location'));
            expect(createRes.headers.location).to.equal(channelUri);
        })

        it('self link is correct', function() {
            expect(createRes.body._links.self.href).to.equal(channelUri);
        })

        // NOTE: _links.latest and _links.ws have their own tests
        it('name is correct', function() {
            expect(createRes.body.name).to.equal(acceptName);
        })

        it('creationDate is correct', function() {
            var returnedDate = moment(createRes.body.creationDate);

            expect(returnedDate.add('minutes', 5).isAfter(moment())).to.be.true;
        })

    })

    describe('Error cases', function() {

        it.skip('Create channel with name reserved by Cassandra', function(done) {
            channelName = 'channelMetadata';

            dhh.createChannel(channelName, function(res){
                if ((res.error) || (!gu.isHTTPSuccess(res.status))) {
                    //console.log('bad things');
                    throw new Error(res.error);
                }
                gu.debugLog('Main test channel:'+ channelName);

                done();
            });
        })

        // See:  https://www.pivotaltracker.com/story/show/49566971
        it('blank name not allowed', function(done){

            dhh.createChannel('', function(res) {
                expect(res.status).to.equal(gu.HTTPresponses.Bad_Request);
                gu.debugLog('Response status: '+ res.status, false);

                done();
            });

        });

        it('name cannot consist only of whitespace', function(done) {

            dhh.createChannel('    ', function(res) {
                expect(res.status).to.equal(gu.HTTPresponses.Bad_Request);

                done();
            })
        })

        it.skip('BUG: https://www.pivotaltracker.com/story/show/51434073 - whitespace is trimmed from name', function(done) {
            var name = dhh.getRandomChannelName();

            dhh.createChannel('  '+ name +'  ', function(res, uri) {
                expect(res.status).to.equal(gu.HTTPresponses.Created);

                dhh.getChannel({uri: uri}, function(getRes, body) {
                    expect(getRes.status).to.equal(gu.HTTPresponses.OK);
                    expect(body.name).to.equal(name);

                    done();
                })
            })
        })

        it.skip('BUG: https://www.pivotaltracker.com/story/show/51434189 -name cannot contain a forward slash', function(done) {
            var name = ranU.randomString(10) + '/'+ ranU.randomString(10);

            dhh.createChannel(name, function(res) {
                expect(gu.isHTTPError(res.status)).to.be.true;
                gu.debugLog('res.status: '+ res.status);
                gu.debugLog('name: '+ name);

                done();
            })
        })

        // https://www.pivotaltracker.com/story/show/46667409
        it('no / empty payload not allowed', function(done) {

            superagent.agent().post(dhh.URL_ROOT +'/channel')
                .set('Content-Type', 'application/json')
                .send('')
                .end(function(err, res) {
                    expect(res.status).to.equal(gu.HTTPresponses.Bad_Request);
                    gu.debugLog('Response status: '+ res.status, DEBUG);

                    done();
                });
        });

        describe('channel names must be unique', function() {

            // TODO: Multiple channels with same name created at same time?

            // https://www.pivotaltracker.com/story/show/44113267
            it('return 409 if attempting to create channel with a name already in use', function(done) {
                dhh.createChannel(channelName, function(res) {
                    expect(res.status).to.equal(gu.HTTPresponses.Conflict);

                    done();
                });
            });

            it('can create two channels whose names differ only in case', function(done) {
                var base = dhh.getRandomChannelName(),
                    firstName = base +'z',
                    secondName = base +'Z';

                dhh.createChannel(firstName, function(firstRes) {
                    expect(firstRes.status).to.equal(gu.HTTPresponses.Created);

                    dhh.createChannel(secondName, function(secondRes) {
                        expect(secondRes.status).to.equal(gu.HTTPresponses.Created);

                        done();
                    })
                })
            })
        })
    })



});