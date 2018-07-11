require('../integration_config');
const {
  fromObjectPath,
  getProp,
} = require('../lib/helpers');

// TODO: let (pun intended) us plan to use const and, uh ... let
var request = require('request');
var channelName = utils.randomChannelName();
var channelResource = channelUrl + "/" + channelName;
var testName = __filename;

/**
 * create a channel
 * post 4 items
 * stream items back with bulk API from all query endpoints, using bulk & batch params
 */
describe(testName, function () {
    /*
      TODO: is this an it block ? if it is more than a helper,
      the logic can stay abstracted but I'm thinking to put the assertion in the actual test file
    */
    utils.putChannel(channelName, false, {"name": channelName, "ttlDays": 1, "tags": ["bulk"]}, testName);

    var multipart =
        'This is a message with multiple parts in MIME format.  This section is ignored.\r\n' +
        '--abcdefg\r\n' +
        'Content-Type: text/plain\r\n' +
        ' \r\n' +
        'message one\r\n' +
        '--abcdefg\r\n' +
        'Content-Type: text/plain\r\n' +
        ' \r\n' +
        'message two\r\n' +
        '--abcdefg\r\n' +
        'Content-Type: text/plain\r\n' +
        ' \r\n' +
        'message three\r\n' +
        '--abcdefg\r\n' +
        'Content-Type: text/plain\r\n' +
        ' \r\n' +
        'message four\r\n' +
        '--abcdefg--'

    var items = [];
    var location = '';

    it("batches items to " + channelName, function (done) {
        request.post({
                url: channelResource + '/bulk',
                headers: {'Content-Type': "multipart/mixed; boundary=abcdefg"},
                body: multipart
            },
            function (err, response, body) {
                expect(err).toBeNull();
                expect(getProp('statusCode', response)).toBe(201);
                location = fromObjectPath(['headers', 'location'], response);
                expect(location).toBeDefined();
                var parse = utils.parseJson(response, testName);
                const responseBody = getProp('body', response);
                console.log(responseBody);
                const uris = fromObjectPath(['_links', 'uris']) || [];
                expect(uris.length).toBe(4);
                items = uris;
                done();
            });
    });

    it("verifies location " + channelName, function (done) {
        request.get({url: location, json: true},
            function (err, response, body) {
                expect(err).toBeNull();
                expect(getProp('statusCode', response)).toBe(200);
                console.log(body);
                const uris = fromObjectPath(['_links', 'uris']) || [];
                expect(uris.length).toBe(4);
                done();
            });
    });

    /*
      TODO: stack traces of assertion failures all lead back to here even when this is called in multiple tests,
      I'm thinking better logging of where the failure originated would make this far more usable
    */
    function getQ(url, param, verifyFunction, accept) {
        return new Promise((resolve, reject) => {
            request.get({
                url: url + param,
                followRedirect: true,
                headers: {Accept: accept}
            }, (err, response, body) => {
                expect(err).toBeNull();
                console.log("url " + url + param + " status=" + getProp('statusCode', response));
                expect(getProp('statusCode', response)).toBe(200);
                verifyFunction(response, param);
                resolve({response: response, body: body});
            });
        });
    }

    function standardVerify(response) {
        for (var i = 0; i < items.length; i++) {
            const body = getProp('body', response) || [];
            expect(body.indexOf(items[i]) > 0).toBe(true);
        }
    }

    function timeVerify(response, param) {
        standardVerify(response);
        var linkHeader = fromObjectPath(['headers', 'link'], response);
        expect(linkHeader).toBeDefined();
        expect(linkHeader).toContain('?stable=false' + param);
        expect(linkHeader).toContain('previous');
    }

    function getAll(url, done, verifyFunction) {
        verifyFunction = verifyFunction || standardVerify;
        var verifyZip = function () {
        };
        url = url + '?stable=false';
        getQ(url, '&bulk=true', verifyFunction, "multipart/mixed")
            .then(function (value) {
                return getQ(url, '&batch=true', verifyFunction, "multipart/mixed");
            })
            .then(function (value) {
                return getQ(url, '&bulk=true', verifyZip, "application/zip");
            })
            .then(function (value) {
                return getQ(url, '&batch=true', verifyZip, "application/zip");
            })
            .then(function (value) {
                done();
            })
    }

    function sliceFromEnd(chars) {
        return items[0].slice(0, items[0].length - chars);
    }

    var timeout = 60 * 1000 * 3;

    it("gets latest bulk " + channelName, function (done) {
        console.log("step 1 " + channelResource)
        getAll(channelResource + '/latest/10', done);
    }, timeout);

    it("gets earliest items " + channelName, function (done) {
        getAll(channelResource + '/earliest/10', done);
    });

    it("gets day items " + channelName, function (done) {
        console.log("step 3 " + sliceFromEnd(26))
        getAll(sliceFromEnd(26), done, timeVerify);
    });

    it("gets hour items " + channelName, function (done) {
        console.log("step 4")
        getAll(sliceFromEnd(23), done, timeVerify);
    });

    it("gets minute items " + channelName, function (done) {
        console.log("step 5")
        getAll(sliceFromEnd(20), done, timeVerify);
    });

    it("gets second items " + channelName, function (done) {
        getAll(sliceFromEnd(17), done, timeVerify);
    });
    it("gets next items " + channelName, function (done) {
        getAll(items[0] + '/next/10', done, function (response, param) {
            for (var i = 1; i < items.length; i++) {
                var responseBody = getProp('body', response) || [];
                expect(responseBody.indexOf(items[i]) > 0).toBe(true);
            }
            var linkHeader = fromObjectPath(['headers', 'link'], response);
            expect(linkHeader).toBeDefined();
            expect(linkHeader).toContain(items[1] + '/previous/10?stable=false' + param);
            expect(linkHeader).toContain(items[3] + '/next/10?stable=false' + param);
        });
    });

    it("gets previous items " + channelName, function (done) {
        getAll(items[3] + '/previous/10', done, function (response, param) {
            for (var i = 0; i < items.length - 1; i++) {
                var responseBody = getProp('body', response) || [];
                expect(responseBody.indexOf(items[i]) > 0).toBe(true);
            }
            var linkHeader = fromObjectPath(['headers', 'link'], response);
            expect(linkHeader).toBeDefined();
            expect(linkHeader).toContain(items[0] + '/previous/10?stable=false' + param);
            expect(linkHeader).toContain(items[2] + '/next/10?stable=false' + param);
        });
    }, timeout);
});
