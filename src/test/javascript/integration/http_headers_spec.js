const request = require('request');
const {
    fromObjectPath,
    getProp,
    hubClientGet
} = require('../lib/helpers');
const {
    getHubUrlBase,
} = require('../lib/config');

const hubUrlBase = getHubUrlBase();
/**
 * verify that X-Forwarded-Host and X-Forwarded-Proto are respected.
 *
 */
describe(__filename, function () {
    it("gets root url ", function (done) {
        console.log("hubUrlBase", hubUrlBase);
        request.get({
            url: hubUrlBase,
            followRedirect: true,
            json: true,
            headers: {
                'X-Forwarded-Host': 'headers',
                'X-Forwarded-Proto': 'https',
            },
        },
        function (err, response, body) {
            expect(err).toBeNull();
            expect(getProp('statusCode', response)).toBe(200);
            console.log(body);
            expect(fromObjectPath(['_links', 'self', 'href'], body)).toBe("https://headers/");
            done();
        });
    });

    it("gets channel url ", function (done) {
        request.get({
            url: hubUrlBase + "/channel",
            followRedirect: true,
            json: true,
            headers: {
                'X-Forwarded-Host': 'headers',
                'X-Forwarded-Proto': 'https',
            },
        },
        function (err, response, body) {
            expect(err).toBeNull();
            expect(getProp('statusCode', response)).toBe(200);
            console.log(body);
            expect(fromObjectPath(['_links', 'self', 'href'], body)).toBe("https://headers/channel");
            done();
        });
    });

    it("gets root url with port ", function (done) {
        console.log("hubUrlBase" + hubUrlBase);
        request.get({
            url: hubUrlBase,
            followRedirect: true,
            json: true,
            headers: {
                'X-Forwarded-Host': 'headers:9000',
                'X-Forwarded-Proto': 'https',
            },
        },
        function (err, response, body) {
            expect(err).toBeNull();
            expect(getProp('statusCode', response)).toBe(200);
            console.log(body);
            expect(fromObjectPath(['_links', 'self', 'href'], body)).toBe("https://headers:9000/");
            done();
        });
    });

    it('returns hub name + port in Hub-Node header', async () => {
        const deployResponse = await hubClientGet(`${hubUrlBase}/internal/deploy`);
        let nodes = getProp('body', deployResponse) || [];
        const rootResponse = await hubClientGet(hubUrlBase);
        console.log(rootResponse.headers);
        const node = fromObjectPath(['headers', 'hub-node'], rootResponse);
        expect(nodes).toContain(node);
    });
});
