var chai = require('chai');
var expect = chai.expect;
var assert = require('chai').assert;
var superagent = require('superagent');
var request = require('request');
var testRandom = require('./js_testing_utils/randomUtils.js');
var async = require('async');
var fs = require('fs');
var http = require('http');

var URL_ROOT = 'http://datahub-01.cloud-east.dev:8080';
var CAT_TOILET_PIC = '/Users/gnewcomb/Pictures/cattoilet.jpg';
var MY_2MB_FILE = '/Users/gnewcomb/Documents/myjs/testing.txt';
var MY_2KB_FILE = '/Users/gnewcomb/Documents/myjs/Iam200kb.txt';

// Test variables that are regularly overwritten
var agent
    , payload
    , spamChannelNames
    , fileAsAStream
    , req
    , uri
    , verAgent;

var channelName = testRandom.randomString(Math.round(Math.random() * 48), testRandom.limitedRandomChar);
var longChannelName = testRandom.randomString(50, testRandom.limitedRandomChar);



// for tracking test dependencies


/*

// For INTERACTIVE testing at the (node) command line


exports.testMe = function () {

 //var happyAgent = superagent.agent();

    console.log('Sending payload:' + payload);


    agent.post(URL_ROOT +'/channel')
        .set('Content-Type', 'application/json')
        .send(payload)
        .end(function(res){
            if (res.error) {
                alert('Suck!'+ res.error.message);
            }
            else {
                console.log("Good times! HTTP 200");
            }
        });
};
*/

before(function(myCallback){
    agent = superagent.agent();
    makeChannel(channelName, function(res){
        if ((res.error) || (res.status != 200)) {
            myCallback(res.error);
        };
        console.log('Main test channel:'+ channelName);
        myCallback();
    });
});

beforeEach(function(){
    agent = superagent.agent();
    payload = uri = req = '';
})

// Helpers
var getValidation = function (myUri, myPayload, myDone)
{
    var myData = '';
    http.get(myUri, function(res) {
        res.on('data', function (chunk) {
            myData += chunk;
        }).on('end', function(){
                expect(myData).to.equal(myPayload);
                myDone();
            });
    }).on('error', function(e) {
            console.log("Got error: " + e.message);
            myDone();
        });
};


var makeChannel = function(myChannelName, myCallback) {

   if (myChannelName.length == 0) {
       throw new Error('Empty channel name not allowed');
   }

    var myPayload = '{"name":"'+ myChannelName +'"}';

    //console.log('POSTING to '+ URL_ROOT +'/channel');
    //console.log('With payload:'+ myPayload);

    agent.post(URL_ROOT +'/channel')
        .set('Content-Type', 'application/json')
        .send(myPayload)
        .end(function(err, res) {
            //console.log('\nCreated channel with name:'+ myChannelName);
            myCallback(res);
        }).on('error', function(e) {
            myCallback(e);
        });
}



describe('Create Channel', function(){

   // 404 trying to GET channel before it exists
    it('should return a 404 trying to GET channel before it exists', function(done){
        agent.get(URL_ROOT +'/channel/'+ testRandom.randomString(Math.round(Math.random() * 48), testRandom.limitedRandomChar))
            .set('Content-Type', 'application/json')
            .end(function(err, res) {
                expect(res.status).to.equal(404);
                done();
            });
    });

    /*  -- Commented out as the creation is now in the before() function.
    it('should return a 200 for channel creation', function(done){
        //console.log('Channel Name: '+ channelName);
        payload = '{"name":"'+ channelName +'"}';

        makeChannel(channelName, function(res) {
            expect(res.status).to.equal(200);
            expect(res.body._links.self.href).to.equal(URL_ROOT +'/channel/'+ channelName);
            done();
        });

    });
    */

    it('should return a 200 trying to GET channel after creation', function(done){

        agent.get(URL_ROOT +'/channel/'+ channelName)
            .set('Content-Type', 'application/json')
            .end(function(err, res) {
                expect(res.status).to.equal(200);
                done();
            });

    });

});



// Tests for this story:
//  Allow a client to save an arbitrary value to a channel.
//  https://www.pivotaltracker.com/story/show/43221623


describe('POST data to channel', function(){


    it('should return a 200 for POSTing data', function(done){

        payload = testRandom.randomString(Math.round(Math.random() * 50));
        uri = URL_ROOT +'/channel/'+ channelName;

        agent.post(uri)
            //.set('Content-Type', 'text/plain')
            .send(payload)
            .end(function(err, res) {
                if (err) throw err;
                expect(res.status).to.equal(200);
                uri = res.body._links.self.href;

                getValidation(uri, payload, done);

             });

    });


    it('POST should return a 404 trying to save to nonexistent channel', function(done){
        payload = testRandom.randomString(Math.round(Math.random() * 50));
        uri = URL_ROOT +'/channel/'+ testRandom.randomString(Math.round(Math.random() * 30), testRandom.limitedRandomChar);
        agent = superagent.agent();

        agent.post(uri)
            .set('Content-Type', 'application/json')
            .send(payload)
            .end(function(err, res) {
                if (err) throw err;
                expect(res.status).to.equal(404);

                done();
            });

    });


    it('POST same set of data twice to channel', function(done){
        payload = testRandom.randomString(Math.round(Math.random() * 50));
        uri = URL_ROOT +'/channel/'+ channelName;
        agent.post(uri)
            .send(payload)
            .end(function(err, res) {
                if (err) throw err;
                expect(res.status).to.equal(200);



                var myAgent = superagent.agent();
                myAgent.post(uri)
                    .send(payload)
                    .end(function(err2, res2){
                        if (err2) throw err2;
                        expect(res2.status).to.equal(200);
                        uri = res.body._links.self.href

                        getValidation(uri, payload, done);
                    });
            });
    });

    it('POST same set of data to two different channels', function(done) {
        var otherChannelName = testRandom.randomString(Math.round(Math.random() * 30), testRandom.limitedRandomChar);

        makeChannel(otherChannelName, function(res) {
            expect(res.status).to.equal(200);
            expect(res.body._links.self.href).to.equal(URL_ROOT +'/channel/'+ otherChannelName);

            payload = testRandom.randomString(Math.round(Math.random() * 50));
            uri = URL_ROOT +'/channel/'+ channelName;
            agent.post(uri)
                .send(payload)
                .end(function(err, res) {
                    if (err) throw err;
                    expect(res.status).to.equal(200);
                    var actualUri = res.body._links.self.href;

                    getValidation(actualUri, payload, function() {

                        uri = URL_ROOT +'/channel/'+ otherChannelName;
                        var agent2 = superagent.agent();
                        agent2.post(uri)
                            .send(payload)
                            .end(function(err2, res2) {
                                if (err2) throw err2;

                                expect(res2.status).to.equal(200);
                                actualUri = res2.body._links.self.href;

                                getValidation(actualUri, payload, done)
                            });

                    });
                });
        });


    });


    it('POST empty data set to channel', function(done){
        payload = '';
        uri = URL_ROOT +'/channel/'+ channelName;
        agent.post(uri)
            .send(payload)
            .end(function(err, res) {
                if (err) throw err;
                expect(res.status).to.equal(200);
                uri = res.body._links.self.href;

                getValidation(uri, payload, done);

            });
    });

    it('POST 200kb file to channel', function(done) {
        uri = URL_ROOT +'/channel/'+ channelName;

        payload = fs.readFileSync(MY_2KB_FILE, "utf8");
        agent.post(uri)
            .send(payload)
            .end(function(err, res) {
                expect(res.status).to.equal(200);
                uri = res.body._links.self.href;

                getValidation(uri, payload, done);
            });
    });

    it('POST 1,000 characters to channel', function(done) {
        uri = URL_ROOT +'/channel/'+ channelName;

        payload = testRandom.randomString(1000, testRandom.simulatedTextChar);

        agent.post(uri)
            .send(payload)
            .end(function(err, res) {
                expect(res.status).to.equal(200);
                uri = res.body._links.self.href;

                getValidation(uri, payload, done);
            });
    });

    //  Can't make this work right yet.
    // TODO: POST binary data
    it.only('POST image file to channel and recover', function(done) {
        uri = URL_ROOT +'/channel/'+ channelName;

        fileAsAStream = fs.createReadStream(CAT_TOILET_PIC);

        fileAsAStream.pipe(request.post(uri,
            function(err, res, body) {
                if (err) {
                    throw err;
                }
                expect(res.statusCode).to.equal(200);
                uri = JSON.parse(body)._links.self.href;
                console.log(uri);

                //getValidation(uri, payload, done);

                done();
            })
        );

    });




    describe.skip('Load tests - POST data', function(){

        var loadChannels = {};
        var loadChannelKeys = [];  // channel.uri (to fetch data) and channel.data, e.g. { con {uri: x, data: y}}

        // To ignore the Loadtest cases:  mocha -R nyan --timeout 4000 --grep Load --invert
        it('Loadtest - POST rapidly to five different channels, then confirm data retrieved via GET is correct', function(done){
            for (var i = 1; i <= 20; i++)
            {
                var thisName = testRandom.randomString(Math.round(Math.random() * 48), testRandom.limitedRandomChar);
                var thisPayload = testRandom.randomString(Math.round(Math.random() * 50));
                loadChannels[thisName] = {"uri":'', "data":thisPayload};

            }

            for (var x in loadChannels){
                if (loadChannels.hasOwnProperty(x)) {
                    loadChannelKeys.push(x);
                }
            }

            async.each(loadChannelKeys, function(cn, callback) {
                makeChannel(cn, function(res) {
                    expect(res.status).to.equal(200);
                    expect(res.body._links.self.href).to.equal(URL_ROOT +'/channel/'+ cn);
                    callback();
                });

            }, function(err) {
                if (err) {
                    throw err;
                };

                async.each(loadChannelKeys, function(cn, callback) {
                    agent = superagent.agent();
                    agent.post(URL_ROOT +'/channel/'+ cn)
                        .send(loadChannels[cn].data)
                        .end(function(err, res) {
                            expect(res.status).to.equal(200);
                            loadChannels[cn].uri = res.body._links.self.href;
                            callback();
                        });
                }, function(err) {
                    if (err) {
                        throw err;
                    };

                    async.eachSeries(loadChannelKeys, function(cn, callback) {
                        uri = loadChannels[cn].uri;
                        payload = loadChannels[cn].data;
                        getValidation(uri, payload, function(){
                            console.log('Confirmed data retrieval from channel: '+ cn);
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

    // TODO: POSTing data should return a creation timestamp

    // TODO: multiple POSTings of data to a channel should return increasing creation timestamps

    // TODO: POST data from different timezone and confirm timestamp is correct?

});



describe.skip('Known failing cases that may not be legitimate', function() {

    it('Should be able to create a channel name of 50 characters', function(done){
        //console.log('Channel Name: '+ longChannelName);
        payload = '{"name":"'+ longChannelName +'"}';

        agent.post(URL_ROOT +'/channel')
            .set('Content-Type', 'application/json')
            .send(payload)
            .end(function(err, res) {
                expect(res.status).to.equal(200);
                expect(res.body._links.self.href).to.equal(URL_ROOT +'/channel/'+ channelName);
                done();
            });
    });


    // !!!! this is stopping / hosing the server...do not run w/o checking with devs first
    it('POST large file (2.5 MB) to channel', function(done) {
        uri = URL_ROOT +'/channel/'+ channelName;

        payload = fs.readFileSync(MY_2MB_FILE);

        agent.post(uri)
            .send(payload)
            .end(function(err, res) {
                expect(res.status).to.equal(200);
                uri = res.body._links.self.href;

                console.log('URI:'+ uri);

                getValidation(uri, payload, done);
            });
    });
});




// TODO:  stress tests -- handled elsewhere?



