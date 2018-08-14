require('../integration_config');
const request = require('request');
const moment = require('moment');
const {
    fromObjectPath,
    getProp,
    hubClientPut,
} = require('../lib/helpers');

const channelName = utils.randomChannelName();
const channelResource = `${channelUrl}/${channelName}`;
const headers = { 'Content-Type': 'application/json' };
const logTime = text => console.log(moment().format('h:mm:ss.SSS'), text);

describe(__filename, function () {
    beforeAll(async () => {
        const response = await hubClientPut(channelResource, headers, { name: channelName, ttlDays: 1 });
        expect(getProp('statusCode', response)).toEqual(201);
    });

    const getItem = (url, status) => {
        const statusCode = status || 200;
        return new Promise((resolve, reject) => {
            request.get({
                url: url + '?stable=false',
                json: true,
            }, (err, response, body) => {
                expect(err).toBeNull();
                expect(getProp('statusCode', response)).toBe(statusCode);
                resolve({response: response, body: body});
            });
        });
    };

    it('adds items and traverses previous links', function (done) {
        const values = [];
        const items = [];
        logTime('starting');
        utils.postItemQ(channelResource)
            .then(function (value) {
                values.push(value);
                const href = fromObjectPath(['body', '_links', 'self', 'href'], value);
                items.push(href);
                logTime('getting self');
                return getItem(href, 200, '0');
            })
            .then(function (value) {
                logTime('getting previousA');
                return getItem(`${items[0]}/previous`, 404, 'A');
            })
            .then(function (value) {
                logTime('getting previousA2');
                return getItem(`${items[0]}/previous/2`, 200, 'B');
            })
            .then(function (value) {
                logTime('posting1');
                const uris = fromObjectPath(['body', '_links', 'uris'], value);
                const urisLength = !!uris && uris.length === 0;
                expect(urisLength).toBe(true);
                return utils.postItemQ(channelResource);
            })
            .then(function (value) {
                logTime('posting2');
                const href = fromObjectPath(['body', '_links', 'self', 'href'], value);
                items.push(href);
                return utils.postItemQ(channelResource);
            })
            .then(function (value) {
                const href = fromObjectPath(['body', '_links', 'self', 'href'], value);
                items.push(href);
                logTime('getting previousB');
                return getItem(`${items[2]}/previous`, 200, 'C');
            })
            .then(function (value) {
                logTime('getting previousB2');
                const href = fromObjectPath(['response', 'request', 'href'], value);
                expect(href).toBe(items[1]);
                return getItem(`${items[2]}/previous/2`, 200);
            })
            .then(function (value) {
                logTime('verifying');
                const href = fromObjectPath(['body', '_links', 'previous', 'href'], value);
                const uris = fromObjectPath(['body', '_links', 'uris'], value) || [];
                expect(uris.length).toBe(2);
                expect(uris[0]).toBe(items[0]);
                expect(uris[1]).toBe(items[1]);
                expect(href).toBe(`${items[0]}/previous/2?stable=false`);
                done();
            });
    }, 2 * 60001);
});
