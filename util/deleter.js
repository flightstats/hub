/*
 This will delete all the test nodes in a Hub, defaulting to dev.
 Run with:
 node deleter.js http://hub
 */
var request = require('request');
var hubUrlBase = process.argv[2] || 'http://hub.svc.dev';
console.info("deleting test channels from " + hubUrlBase);

request.get({url: hubUrlBase + "/channel", headers: {"Content-Type": "application/json"}},
    function (err, response, body) {
        if (response.statusCode !== 200) {
            console.info("unable to get channel information" + response);
        }
        console.info("got response: " + body);
        var channels = JSON.parse(body)['_links']['channels'];
        for (var i = 0; i < channels.length; i++) {
            var channel = channels[i];

            if (channel.name.substring(0, 7) == "test_0_") {
                console.info("deleting " + JSON.stringify(channel));
                request.del({url: channel.href},
                    function (err, response, body) {
                        if (response.statusCode !== 200) {
                            console.info("unable to delete " + response);
                        }
                    })
            }
        }


    });
