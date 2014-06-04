require('./integration_config.js');

var request = require('request');
var http = require('http');
var channelName = utils.randomChannelName();
var groupName = utils.randomChannelName();
var channelResource = channelUrl + "/" + channelName;
var testName = __filename;
var groupConfig = {
    callbackUrl : 'http://localhost:8888/',
    channelUrl: channelResource,
    transactional: true
};

/**
 * This should:
 *
 * 1 - create a channel
 * 2 - create a group on that channel
 *      make sure there is a listener at the endpoint
 * 3 - get the group
 * 4 - insert records into the channel
 * 5 - verify that the records are returned within delta time
 * 6 - delete the group
 */
describe(testName, function () {
    utils.createChannel(channelName);

    utils.sleep(500);

    utils.putGroup(groupName, groupConfig);

    it('runs callback server', function () {
        var started = false;
        var called = false;
        var server;
        var closed = false;

        runs(function () {
            server = http.createServer(function (request, response) {
                console.info('got request');
                //console.info(request);
                response.writeHead(200);
                response.end();
                called = true;
            });

            server.on('connection', function(socket) {
                socket.setTimeout(2000);
            });

            server.listen(8888, function () {
                console.info('Express server listening on port 8888');
                started = true;
            });
        });

        waitsFor(function() {
            return started;
        }, 11000);

        runs(function () {
            //todo - gfm - 6/4/14 - post more items
            request.post({url: channelResource,
                    headers: {"Content-Type": "application/json", user: 'somebody' },
                    body: JSON.stringify({ "data": Date.now()})},
                function (err, response, body) {
                    expect(err).toBeNull();
                    expect(response.statusCode).toBe(201);
                });
        });

        waitsFor(function() {
            //todo - gfm - 6/4/14 - make this a list with length
            return called;
        }, 12000);

        runs(function () {
            server.close(function () {
                console.info('closed!');
                closed = true;
            });
        });

        waitsFor(function() {
            return closed;
        }, 9000);

    });



    //
    //utils.addItem(channelResource);






});

