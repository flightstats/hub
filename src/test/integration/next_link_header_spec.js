require('./integration_config.js');

var channelName = utils.randomChannelName();
var channelResource = channelUrl + '/' + channelName;

describe(__filename, function () {

    it('creates a channel', function (done) {
        var url = channelUrl;
        var headers = {'Content-Type': 'application/json'};
        var body = {'name': channelName};

        utils.httpPost(url, headers, body)
            .then(function (response) {
                expect(response.statusCode).toEqual(201);
            })
            .catch(function (error) {
                expect(error).toBeNull();
            })
            .fin(function () {
                done();
            });
    });

    var firstItemURL;

    it('inserts the first item', function (done) {
        var url = channelResource;
        var headers = {'Content-Type': 'text/plain'};
        var body = 'FIRST ITEM';

        utils.httpPost(url, headers, body)
            .then(function (response) {
                expect(response.statusCode).toEqual(201);
                firstItemURL = reponse.body._links.self.href;
            })
            .catch(function (error) {
                expect(error).toBeNull();
            })
            .fin(function () {
                done();
            });
    });

    it('verifies the first item doesn\'t have a next link', function (done) {
        utils.httpGet(firstItemURL)
            .then(function (response) {
                expect(response.statusCode).toEqual(200);
                expect(response.headers['link']).toNotContain('next');
            })
            .catch(function (error) {
                expect(error).toBeNull();
            })
            .fin(function () {
                done();
            });
    });

    it('inserts the second item', function (done) {
        var url = channelResource;
        var headers = {'Content-Type': 'text/plain'};
        var body = 'SECOND ITEM';

        utils.httpPost(url, headers, body)
            .then(function (response) {
                expect(response.statusCode).toEqual(201);
            })
            .catch(function (error) {
                expect(error).toBeNull();
            })
            .fin(function () {
                done();
            });
    });

    it('verifies the first item does have a next link', function (done) {
        utils.httpGet(firstItemURL)
            .then(function (response) {
                expect(response.statusCode).toEqual(200);
                expect(response.headers['link']).toContain('next');
            })
            .catch(function (error) {
                expect(error).toBeNull();
            })
            .fin(function () {
                done();
            });
    });

});
