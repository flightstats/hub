const request = require('request');
const {
    fromObjectPath,
    getProp,
    hubClientDelete,
    hubClientPut,
    parseJson,
    randomChannelName,
} = require('../lib/helpers');
const {
    getChannelUrl,
} = require('../lib/config');

const headers = { 'Content-Type': 'application/json' };
const channelName = randomChannelName();
const channelResource = `${getChannelUrl()}/${channelName}`;
const multipart = [
    'This is a message with multiple parts in MIME format.  This section is ignored.\r\n',
    '--abcdefg\r\n',
    'Content-Type: text/plain\r\n',
    ' \r\n',
    'message one\r\n',
    '--abcdefg\r\n',
    'Content-Type: text/plain\r\n',
    ' \r\n',
    'message two\r\n',
    '--abcdefg\r\n',
    'Content-Type: text/plain\r\n',
    ' \r\n',
    'message three\r\n',
    '--abcdefg\r\n',
    'Content-Type: text/plain\r\n',
    ' \r\n',
    'message four\r\n',
    '--abcdefg--',
].join('');
let channelCreated = false;
/**
 * create a channel
 * post 4 items
 * stream items back with bulk API from all query endpoints, using bulk & batch params
 */
describe(__filename, function () {
    beforeAll(async () => {
        const body = {
            ttlDays: 1,
            tags: ['bulk'],
        };
        const url = `${getChannelUrl()}/${channelName}`;
        const response = await hubClientPut(url, headers, body);
        if (getProp('statusCode', response) === 201) {
            channelCreated = true;
        }
    });

    let items = [];
    let location = '';

    it("batches items to " + channelName, function (done) {
        if (!channelCreated) return done.fail('channel not created in before block');
        request.post({
            url: channelResource + '/bulk',
            headers: {'Content-Type': "multipart/mixed; boundary=abcdefg"},
            body: multipart,
        },
        function (err, response, body) {
            expect(err).toBeNull();
            expect(getProp('statusCode', response)).toBe(201);
            const location = response.header('location');
            expect(location).toBeDefined();
            const parse = parseJson(response, __filename);
            const responseBody = getProp('body', response);
            console.log(responseBody);
            const uris = fromObjectPath(['_links', 'uris'], parse) || [];
            expect(uris.length).toBe(4);
            items = uris;
            done();
        });
    });

    it("verifies location " + channelName, function (done) {
        if (!channelCreated) return done.fail('channel not created in before block');
        request.get({url: location, json: true},
            function (err, response, body) {
                expect(err).toBeNull();
                expect(getProp('statusCode', response)).toBe(200);
                console.log('verify location response body: ', body);
                const uris = fromObjectPath(['_links', 'uris'], body) || [];
                expect(uris.length).toBe(4);
                done();
            });
    });

    /*
      TODO: stack traces of assertion failures all lead back to here even when this is called in multiple tests,
      I'm thinking better logging of where the failure originated would make this far more usable
    */
    function getQ (url, param, verifyFunction, accept) {
        return new Promise((resolve, reject) => {
            request.get({
                url: url + param,
                followRedirect: true,
                headers: {Accept: accept},
            }, (err, response, body) => {
                expect(err).toBeNull();
                console.log("url " + url + param + " status=" + getProp('statusCode', response));
                expect(getProp('statusCode', response)).toBe(200);
                verifyFunction(response, param);
                resolve({response: response, body: body});
            });
        });
    }

    function standardVerify (response) {
        for (let i = 0; i < items.length; i++) {
            const body = getProp('body', response) || [];
            expect(body.indexOf(items[i]) > 0).toBe(true);
        }
    }

    function timeVerify (response, param) {
        standardVerify(response);
        const linkHeader = response.header('link');
        expect(linkHeader).toBeDefined();
        expect(linkHeader).toContain('?stable=false' + param);
        expect(linkHeader).toContain('previous');
    }

    function getAll (url, done, verifyFunction) {
        verifyFunction = verifyFunction || standardVerify;
        const verifyZip = function () {
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
            });
    }

    function sliceFromEnd (chars) {
        return (items[0] || '').slice(0, (items[0] || '').length - chars);
    }

    const timeout = 60 * 1000 * 3;

    it("gets latest bulk " + channelName, function (done) {
        if (!channelCreated) return done.fail('channel not created in before block');
        console.log("step 1 " + channelResource);
        getAll(channelResource + '/latest/10', done);
    }, timeout);

    it("gets earliest items " + channelName, function (done) {
        if (!channelCreated) return done.fail('channel not created in before block');
        getAll(channelResource + '/earliest/10', done);
    });

    it("gets day items " + channelName, function (done) {
        if (!channelCreated) return done.fail('channel not created in before block');
        console.log("step 3 " + sliceFromEnd(26));
        getAll(sliceFromEnd(26), done, timeVerify);
    });

    it("gets hour items " + channelName, function (done) {
        if (!channelCreated) return done.fail('channel not created in before block');
        console.log("step 4");
        getAll(sliceFromEnd(23), done, timeVerify);
    });

    it("gets minute items " + channelName, function (done) {
        if (!channelCreated) return done.fail('channel not created in before block');
        console.log("step 5");
        getAll(sliceFromEnd(20), done, timeVerify);
    });

    it("gets second items " + channelName, function (done) {
        if (!channelCreated) return done.fail('channel not created in before block');
        getAll(sliceFromEnd(17), done, timeVerify);
    });
    it("gets next items " + channelName, function (done) {
        if (!channelCreated) return done.fail('channel not created in before block');
        getAll(items[0] + '/next/10', done, function (response, param) {
            for (let i = 1; i < items.length; i++) {
                const responseBody = getProp('body', response) || [];
                expect(responseBody.indexOf(items[i]) > 0).toBe(true);
            }
            const linkHeader = response.header('link');
            expect(linkHeader).toBeDefined();
            expect(linkHeader).toContain(items[1] + '/previous/10?stable=false' + param);
            expect(linkHeader).toContain(items[3] + '/next/10?stable=false' + param);
        });
    });

    it("gets previous items " + channelName, function (done) {
        if (!channelCreated) return done.fail('channel not created in before block');
        getAll(items[3] + '/previous/10', done, function (response, param) {
            for (let i = 0; i < items.length - 1; i++) {
                const responseBody = getProp('body', response) || [];
                expect(responseBody.indexOf(items[i]) > 0).toBe(true);
            }
            const linkHeader = response.header('link');
            expect(linkHeader).toBeDefined();
            expect(linkHeader).toContain(items[0] + '/previous/10?stable=false' + param);
            expect(linkHeader).toContain(items[2] + '/next/10?stable=false' + param);
        });
    }, timeout);

    afterAll(async () => {
        await hubClientDelete(channelResource);
    });
});
