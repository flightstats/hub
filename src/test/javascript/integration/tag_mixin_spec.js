const request = require('request');
const parse = require('parse-link-header');
const {
    fromObjectPath,
    getProp,
    hubClientChannelRefresh,
    hubClientDelete,
    hubClientPostTestItem,
    hubClientPut,
    parseJson,
    randomChannelName,
} = require('../lib/helpers');
const {
    getChannelUrl,
    getHubUrlBase,
} = require('../lib/config');

const channelUrl = getChannelUrl();
const hubUrlBase = getHubUrlBase();
const channelA = randomChannelName();
const channelB = randomChannelName();
const headers = { 'Content-Type': 'application/json' };
const tag = Math.random().toString().replace(".", "");
const channelBody = {
    tags: [tag, "test"],
    ttlDays: 1,
};
let uris = [];
const tagUrl = `${hubUrlBase}/tag/${tag}`;
let parsedLinks = null;
const linkStripParams = uri => uri.substr(0, uri.indexOf('?'));
/**
 * This should:
 * Create ChannelA with tag TagA
 * Create ChannelB with tag TagA
 *
 * Add data to channel
 * Add data to channelB
 *
 * verify that tag time query can get data back out
 *
 */
describe(__filename, function () {
    beforeAll(async () => {
        const responseA = await hubClientPut(`${channelUrl}/${channelA}`, headers, channelBody);
        const responseB = await hubClientPut(`${channelUrl}/${channelB}`, headers, channelBody);
        expect(getProp('statusCode', responseA)).toEqual(201);
        expect(getProp('statusCode', responseB)).toEqual(201);
        const responseC = await hubClientChannelRefresh();
        expect(getProp('statusCode', responseC)).toEqual(200);
    });

    const traverse = (uri, index, done) => {
        const url = uri.trim();
        console.log(` traverse ${url} ${__filename}`);
        request.get({ url },
            function (err, response, body) {
                expect(err).toBeNull();
                expect(getProp('statusCode', response)).toBe(200);
                if (getProp('statusCode', response) === 200) {
                    body = parseJson(response, __filename);
                    const links = fromObjectPath(['headers', 'link'], response) || '{}';
                    parsedLinks = parse(links);
                    const item = uris[index] && linkStripParams(uris[index]);
                    if (parsedLinks) {
                        const prevUrl = fromObjectPath(['previous', 'url'], parsedLinks);
                        const nextUrl = fromObjectPath(['next', 'url'], parsedLinks);
                        expect(prevUrl).toContain(`${item}/previous?tag=${tag}`);
                        expect(nextUrl).toContain(`${item}/next?tag=${tag}`);
                    } else {
                        expect(parsedLinks).toBe(true);
                    }
                }
                done();
            });
    };

    it('posts items to the channel', async () => {
        const response1 = await hubClientPostTestItem(`${channelUrl}/${channelA}`);
        expect(getProp('statusCode', response1)).toEqual(201);

        const response2 = await hubClientPostTestItem(`${channelUrl}/${channelB}`);
        expect(getProp('statusCode', response2)).toEqual(201);

        const response3 = await hubClientPostTestItem(`${channelUrl}/${channelA}`);
        expect(getProp('statusCode', response3)).toEqual(201);
    });

    it(`gets tag hour ${tag}`, function (done) {
        const url = `${tagUrl}/time/hour?stable=false&trace=true'`;
        console.log('calling tag hour ', url);
        request.get({ url, headers },
            function (err, response, body) {
                expect(err).toBeNull();
                const statusCode = getProp('statusCode', response);
                expect(statusCode).toBe(200);
                if (statusCode === 200) {
                    body = parseJson(response, __filename);
                    console.log('parsed tag body', body);
                    const links = getProp('_links', body);
                    uris = getProp('uris', links) || [];
                    expect(uris.length).toBe(3);
                    expect(uris[0]).toContain(channelA);
                    expect(uris[1]).toContain(channelB);
                    expect(uris[2]).toContain(channelA);
                } else {
                    console.log(`failing test, can't get uris. status: ${statusCode}`);
                }
                done();
            });
    }, 2 * 60001);

    it('gets last link ', function (done) {
        traverse(`${uris[2]}&stable=false`, 2, done);
    }, 60002);

    it('gets previous link ', function (done) {
        const prevUrl = fromObjectPath(['previous', 'url'], parsedLinks);
        traverse(prevUrl, 1, done);
    }, 60003);

    it('gets 2nd previous link ', function (done) {
        const prevUrl = fromObjectPath(['previous', 'url'], parsedLinks);
        traverse(prevUrl, 0, done);
    }, 60004);

    it('gets first link ', function (done) {
        traverse(uris[0], 0, done);
    }, 60005);

    it('gets next link ', function (done) {
        const nextUrl = fromObjectPath(['next', 'url'], parsedLinks);
        traverse(`${nextUrl}&stable=false`, 1, done);
    }, 60006);

    it('gets 2nd next link ', function (done) {
        const nextUrl = fromObjectPath(['next', 'url'], parsedLinks);
        traverse(`${nextUrl}&stable=false`, 2, done);
    }, 60007);

    it("gets latest unstable in tag ", function (done) {
        request.get({url: `${tagUrl}/latest?stable=false`, followRedirect: false},
            function (err, response, body) {
                expect(err).toBeNull();
                expect(getProp('statusCode', response)).toBe(303);
                const location = fromObjectPath(['headers', 'location'], response);
                expect(location).toBe(uris[2]);
                done();
            });
    }, 60008);

    it("gets latest N unstable in tag ", function (done) {
        request.get({url: `${tagUrl}/latest/10?stable=false`},
            function (err, response, body) {
                expect(err).toBeNull();
                expect(getProp('statusCode', response)).toBe(200);
                const parsed = parseJson(response, __filename);
                const currentUris = fromObjectPath(['_links', 'uris'], parsed) || [];
                expect(currentUris.length).toBe(3);
                currentUris.forEach(function (uri, index) {
                    expect(uris[index]).toBe(uri);
                });

                done();
            });
    }, 60009);

    it(`gets earliest unstable in channel ${tag}`, function (done) {
        request.get({url: `${tagUrl}/earliest?stable=false&trace=true`, followRedirect: false},
            function (err, response, body) {
                expect(err).toBeNull();
                expect(getProp('statusCode', response)).toBe(303);
                const location = fromObjectPath(['headers', 'location'], response);
                expect(location).toBe(uris[0]);
                done();
            });
    }, 60010);

    it(`next from item ${tag}`, function (done) {
        const url = `${linkStripParams(uris[0])}/next/2?tag=${tag}&stable=false`;
        request.get({url: url},
            function (err, response, body) {
                expect(err).toBeNull();
                expect(getProp('statusCode', response)).toBe(200);
                const parsed = parseJson(response, __filename);
                const currentUris = fromObjectPath(['_links', 'uris'], parsed) || [];
                expect(currentUris.length).toBe(2);
                currentUris.forEach(function (uri, index) {
                    console.log('found ', uri);
                    expect(uri).toBe(uris[index + 1]);
                });
                done();
            });
    }, 60011);

    it(`previous from tag ${tag}`, function (done) {
        const substringLength = uris[2].indexOf(channelA) + channelA.length;
        const last = linkStripParams(uris[2]).substring(substringLength);
        const url = `${tagUrl}${last}/previous/3?tag=${tag}&stable=false`;
        request.get({ url },
            function (err, response, body) {
                expect(err).toBeNull();
                expect(getProp('statusCode', response)).toBe(200);
                const parsed = parseJson(response, __filename);
                const currentUris = fromObjectPath(['_links', 'uris'], parsed) || [];
                expect(currentUris.length).toBe(2);
                currentUris.forEach(function (uri, index) {
                    console.log('found ', uri);
                    expect(uri).toBe(uris[index]);
                });

                done();
            });
    }, 60012);

    it(`next from tag ${tag}`, function (done) {
        const substringLength = uris[0].indexOf(channelA) + channelA.length;
        const last = linkStripParams(uris[0]).substring(substringLength);
        const url = `${tagUrl}${last}/next/3?tag=${tag}&stable=false`;
        request.get({ url },
            function (err, response, body) {
                expect(err).toBeNull();
                expect(getProp('statusCode', response)).toBe(200);
                const parsed = parseJson(response, __filename);
                const currentUris = fromObjectPath(['_links', 'uris'], parsed) || [];
                expect(currentUris.length).toBe(2);
                currentUris.forEach(function (uri, index) {
                    console.log('found ', uri);
                    expect(uri).toBe(uris[index + 1]);
                });
                done();
            });
    }, 60013);

    it(`previous from item ${tag}`, function (done) {
        const url = `${linkStripParams(uris[2])}/previous/2?tag=${tag}&stable=false`;
        request.get({ url },
            function (err, response, body) {
                expect(err).toBeNull();
                expect(getProp('statusCode', response)).toBe(200);
                const parsed = parseJson(response, __filename);
                const currentUris = fromObjectPath(['_links', 'uris'], parsed) || [];
                expect(currentUris.length).toBe(2);
                currentUris.forEach(function (uri, index) {
                    console.log('found ', uri);
                    expect(uri).toBe(uris[index]);
                });

                done();
            });
    }, 60014);

    it(`latest from channel ${tag}`, function (done) {
        const url = `${hubUrlBase}/channel/${channelB}/latest?tag=${tag}&stable=false`;
        request.get({ url, followRedirect: false },
            function (err, response, body) {
                expect(err).toBeNull();
                const location = fromObjectPath(['headers', 'location'], response);
                expect(getProp('statusCode', response)).toBe(303);
                expect(location).toBe(uris[2]);
                done();
            });
    }, 60015);

    it(`latest 3 from channel ${tag}`, function (done) {
        const url = `${hubUrlBase}/channel/${channelA}/latest/3?tag=${tag}&stable=false`;
        request.get({ url, followRedirect: false },
            function (err, response, body) {
                expect(err).toBeNull();
                expect(getProp('statusCode', response)).toBe(200);
                const parsed = parseJson(response, __filename);
                const currentUris = fromObjectPath(['_links', 'uris'], parsed) || [];
                expect(currentUris.length).toBe(3);
                currentUris.forEach(function (uri, index) {
                    console.log('found ', uri);
                    expect(uri).toBe(uris[index]);
                });
                done();
            });
    }, 60016);

    it(`earliest from channel ${tag}`, function (done) {
        const url = `${hubUrlBase}/channel/${channelB}/earliest?tag=${tag}&stable=false&trace=true`;
        request.get({ url, followRedirect: false },
            function (err, response, body) {
                expect(err).toBeNull();
                expect(getProp('statusCode', response)).toBe(303);
                const location = fromObjectPath(['headers', 'location'], response);
                expect(location).toBe(uris[0]);
                done();
            });
    }, 60017);

    it(`earliest 3 from channel ${tag}`, function (done) {
        const url = `${hubUrlBase}/channel/${channelA}/earliest/3?tag=${tag}&stable=false&trace=true`;
        request.get({ url, followRedirect: false },
            function (err, response, body) {
                expect(err).toBeNull();
                expect(getProp('statusCode', response)).toBe(200);
                const parsed = parseJson(response, __filename);
                const currentUris = fromObjectPath(['_links', 'uris'], parsed) || [];
                expect(currentUris.length).toBe(3);
                currentUris.forEach(function (uri, index) {
                    console.log('found ', uri);
                    expect(uri).toBe(uris[index]);
                });
                done();
            });
    }, 60018);

    it(`day query from channel ${tag}`, function (done) {
        const url = `${hubUrlBase}/channel/${channelB}/time/day?tag=${tag}&stable=false`;
        request.get({ url },
            function (err, response, body) {
                expect(err).toBeNull();
                expect(getProp('statusCode', response)).toBe(200);
                const parsed = parseJson(response, __filename);
                console.log('parsed', parsed);
                const currentUris = fromObjectPath(['_links', 'uris'], parsed) || [];
                expect(currentUris.length).toBe(3);
                currentUris.forEach(function (uri, index) {
                    console.log('found ', uri);
                    expect(uri).toBe(uris[index]);
                });
                done();
            });
    }, 60019);

    afterAll(async () => {
        await hubClientDelete(`${channelUrl}/${channelA}`);
        await hubClientDelete(`${channelUrl}/${channelB}`);
    });
});
