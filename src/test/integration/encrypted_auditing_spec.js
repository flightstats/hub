require('./integration_config.js');

var request = require('request');
var channelName = utils.randomChannelName();
var channelResource = channelUrl + "/" + channelName;
var auditChannelResource = channelResource + '_audit';
var testName = __filename;
var user = 'nobody';
var foundAudits = []

/**
 * //todo - gfm - 5/15/14 - prevent this from running in the normal Hub
 *
 * This should:
 * 1 - create a channel in the encrypted hub
 * 2 - insert two records into the channel
 * 3 - read both records with different users
 * 4 - verify that 4 audit records exist
 */
describe(testName, function () {
    utils.createChannel(channelName);

    it("verifies audit channel exists with tag " + auditChannelResource, function (done) {
        request.get({url: auditChannelResource },
            function (err, response, body) {
                expect(err).toBeNull();
                expect(response.statusCode).toBe(200);
                var parse = JSON.parse(body);
                expect(parse.name).toBe(channelName + '_audit');
                expect(parse.tags).toContain('audit');
                done();
            });
    });

    utils.addItem(channelResource);
    utils.addItem(channelResource);

    getItem(1000, 'nobody');
    getItem(1001, 'anybody');
    getItem(1001, 'nobody');
    getItem(1000, 'anybody');

    verifyAuditing(1000);
    verifyAuditing(1001);
    verifyAuditing(1002);
    verifyAuditing(1003);

    it('verifies all audits are found', function() {
        expect(foundAudits.length).toEqual(4);
        expect(foundAudits).toContain(channelResource + '/1000nobody' );
        expect(foundAudits).toContain(channelResource + '/1001nobody');
        expect(foundAudits).toContain(channelResource + '/1001anybody');
        expect(foundAudits).toContain(channelResource + '/1000anybody');
    });

    //todo - gfm - 5/15/14 - verify auditing channel isn't audited itself

    //todo - gfm - 5/15/14 - prevent inserts to audited channels

    //todo - gfm - 5/15/14 - test creating channel ending in _audit

    function verifyAuditing(id) {
        var url = auditChannelResource + '/' + id;
        it("verifies auditing " + url, function (done) {
            request.get({url: url },
                function (err, response, body) {
                    expect(err).toBeNull();
                    expect(response.statusCode).toBe(200);
                    var parse = JSON.parse(body);
                    foundAudits.push(parse.uri + parse.user);
                    done();
                });
        });
    }

    function getItem(id, user) {
        var url = channelResource + '/' + id;
        it("gets an item as a user " + url, function (done) {
            request.get({url: url, headers: { User: user } },
                function (err, response, body) {
                    expect(err).toBeNull();
                    expect(response.statusCode).toBe(200);
                    expect(response.headers.user).toBe('somebody');
                    done();
                });
        });
    }

});

