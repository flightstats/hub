require('./integration_config');
const moment = require('moment');

describe(__filename, function () {

    /**
     * POST a single item, GET it, and verify the "X-Item-Length"
     * header is present with the correct value
     */

    describe('single item length', function () {
        var channelName = utils.randomChannelName();
        var channelEndpoint = channelUrl + '/' + channelName;
        var itemHeaders = {'Content-Type': 'text/plain'};
        var itemContent = 'this string has normal letters, and unicode characters like "\u03B1"';
        var itemURL;

        utils.createChannel(channelName, null, 'single inserts');

        it('posts a single item', function (done) {
            utils.postItemQwithPayload(channelEndpoint, itemHeaders, itemContent)
                .then(function (result) {
                    expect(function () {
                        var json = JSON.parse(result.body);
                        itemURL = json._links.self.href;
                    }).not.toThrow();
                    done();
                });
        });

        it('verifies item has correct length info', function (done) {
            expect(itemURL !== undefined).toBe(true);
            utils.getItem(itemURL, function (headers, body) {
                expect('x-item-length' in headers).toBe(true);
                var bytes = Buffer.from(itemContent, 'utf-8').length;
                expect(headers['x-item-length']).toBe(bytes.toString());
                expect(body.toString()).toEqual(itemContent);
                done();
            });
        });
    });

    /**
     * POST bulk items, GET each one, and verify the "X-Item-Length"
     * header is present with the correct values
     */

    describe('bulk item length', function () {
        var channelName = utils.randomChannelName();
        var channelEndpoint = channelUrl + '/' + channelName + '/bulk';
        var bulkHeaders = {'Content-Type': 'multipart/mixed; boundary=oxoxoxo'};
        var itemOneContent = '{"foo":"bar"}';
        var itemTwoContent = 'foo, bar?';
        var bulkContent =
            '--oxoxoxo\r\n' +
            'Content-Type: application/json\r\n' +
            '\r\n' + itemOneContent + '\r\n' +
            '--oxoxoxo\r\n' +
            'Content-Type: text/plain\r\n' +
            '\r\n' + itemTwoContent + '\r\n' +
            '--oxoxoxo--';
        var itemURLs = [];

        utils.createChannel(channelName, null, 'bulk inserts');

        it('posts items in bulk', function (done) {
            utils.postItemQwithPayload(channelEndpoint, bulkHeaders, bulkContent)
                .then(function (result) {
                    expect(function () {
                        var json = JSON.parse(result.body);
                        itemURLs = json._links.uris;
                    }).not.toThrow();
                    expect(itemURLs.length).toBe(2);
                    done();
                });
        });

        it('verifies first item has correct length info', function (done) {
            utils.getItem(itemURLs[0], function (headers, body) {
                expect('x-item-length' in headers).toBe(true);
                var bytes = Buffer.from(itemOneContent, 'utf-8').length;
                expect(headers['x-item-length']).toBe(bytes.toString());
                expect(body.toString()).toEqual(itemOneContent);
                done();
            });
        });

        it('verifies second item has correct length info', function (done) {
            utils.getItem(itemURLs[1], function (headers, body) {
                expect('x-item-length' in headers).toBe(true);
                var bytes = Buffer.from(itemTwoContent, 'utf-8').length;
                expect(headers['x-item-length']).toBe(bytes.toString());
                expect(body.toString()).toEqual(itemTwoContent);
                done();
            });
        });
    });

    /**
     * POST a large item, GET it, and verify the "X-Item-Length"
     * header is present with the correct value
     */

    describe('large item length', function () {
        var channelName = utils.randomChannelName();
        var channelEndpoint = channelUrl + '/' + channelName;
        var itemHeaders = {'Content-Type': 'text/plain'};
        var itemSize = 41 * 1024 * 1024;
        var itemContent = Array(itemSize).join('a');
        var itemURL;

        utils.createChannel(channelName, null, 'large inserts');

        it('posts a large item', function (done) {
            utils.postItemQwithPayload(channelEndpoint, itemHeaders, itemContent)
                .then(function (result) {
                    expect(function () {
                        var json = JSON.parse(result.body);
                        itemURL = json._links.self.href;
                    }).not.toThrow();
                    done();
                });
        });

        it('verifies item has correct length info', function (done) {
            expect(itemURL !== undefined).toBe(true);
            utils.getItem(itemURL, function (headers, body) {
                console.log('headers:', headers);
                expect('x-item-length' in headers).toBe(true);
                expect(headers['x-item-length']).toEqual(itemSize);
                expect(body.toString()).toEqual(itemContent);
                done();
            });
        });
    });

    /**
     * POST a historical item, GET it, and verify the "X-Item-Length"
     * header is present with the correct value
     */

    describe('historical item length', function () {
        var oneDayAgo = moment().subtract(1, 'days');
        var pathPattern = 'YYYY/MM/DD/HH/mm/ss/SSS';
        var channelName = utils.randomChannelName();
        var channelEndpoint = channelUrl + '/' + channelName + '/' + oneDayAgo.format(pathPattern);
        var itemHeaders = {'Content-Type': 'text/plain'};
        var itemContent = 'this is a string for checking length on historical inserts';
        var itemURL;

        utils.putChannel(channelName, null, {"mutableTime": moment().toISOString()}, 'historical inserts', null);

        it('posts a historical item', function (done) {
            utils.postItemQwithPayload(channelEndpoint, itemHeaders, itemContent)
                .then(function (result) {
                    expect(function () {
                        var json = JSON.parse(result.body);
                        itemURL = json._links.self.href;
                    }).not.toThrow();
                    done();
                });
        });

        it('verifies item has correct length info', function (done) {
            expect(itemURL !== undefined).toBe(true);
            utils.getItem(itemURL, function (headers, body) {
                expect('x-item-length' in headers).toBe(true);
                var bytes = Buffer.from(itemContent, 'utf-8').length;
                expect(headers['x-item-length']).toBe(bytes.toString());
                expect(body.toString()).toEqual(itemContent);
                done();
            });
        });
    });

});
