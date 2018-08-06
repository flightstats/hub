// require('../integration_config');
const test = require('ava');
const {
    fromObjectPath,
    getHubDomain,
    getProp,
    hubClientDelete,
    hubClientGet,
    hubClientPost,
    hubClientPut,
    randomChannelName,
} = require('../lib/helpers');

const hubDomain = getHubDomain();
const channelUrl = `http://${hubDomain}/channel`;
const channelName = randomChannelName();
const channelResource = `${channelUrl}/${channelName}/bulk`;
const headers = { 'Content-Type': 'application/json' };
const multipart = [
    'This is a message with multiple parts in MIME format.  This section is ignored.\r\n',
    '--abcdefg\r\n',
    'Content-Type: application/xml\r\n',
    ' \r\n',
    '<coffee><roast>french</roast><coffee>\r\n',
    '--abcdefg\r\n',
    'Content-Type: application/json\r\n',
    ' \r\n',
    '{ "type" : "coffee", "roast" : "french" }\r\n',
    '--abcdefg--',
].join('');

test.before(async t => {
    // create channel
    const channelBody = JSON.stringify({ ttlDays: 1, tags: ['bulk'] });
    const response = await hubClientPut(`${channelUrl}/${channelName}`, headers, channelBody);
    t.is(getProp('statusCode', response), 201);

    // post multipart item to channel bulk
    const contentTypeMulti = { 'Content-Type': "multipart/mixed; boundary=abcdefg" };
    const postResponse = await hubClientPost(channelResource, contentTypeMulti, multipart);
    t.is(getProp('statusCode', postResponse), 201);
    const body = getProp('body', postResponse);
    // set the locations of the items on context
    t.context.uris = fromObjectPath(['_links', 'uris'], body) || [];
});

test('get and verify first multipart item', async (t) => {
    try {
        const response = await hubClientGet(t.context.uris[0]);
        t.is(getProp('statusCode', response), 200);
        t.is(getProp('body', response), '<coffee><roast>french</roast><coffee>');
        const contentType = fromObjectPath(['headers', 'content-type'], response);
        t.is(contentType, 'application/xml');
    } catch (ex) {
        t.fail(ex);
    }
});

test('get and verify second multipart item', async (t) => {
    try {
        const response = await hubClientGet(t.context.uris[1]);
        t.is(getProp('statusCode', response), 200);
        t.is(getProp('body', response), '{ "type" : "coffee", "roast" : "french" }');
        const contentType = fromObjectPath(['headers', 'content-type'], response);
        t.is(contentType, 'application/json');
    } catch (ex) {
        t.fail(ex);
    }
});

test('get item from previous', async (t) => {
    try {
        const response = await hubClientGet(`${t.context.uris[1]}/previous?trace=true&stable=false'`);
        const location = fromObjectPath(['headers', 'location'], response);
        t.is(getProp('statusCode', response), 303);
        t.is(location, t.context.uris[0]);
    } catch (ex) {
        t.fail(ex);
    }
});

test('get item from next', async (t) => {
    try {
        const response = await hubClientGet(`${t.context.uris[0]}/next?trace=true&stable=false'`);
        const location = fromObjectPath(['headers', 'location'], response);
        t.is(getProp('statusCode', response), 303);
        t.is(location, t.context.uris[1]);
    } catch (ex) {
        t.fail(ex);
    }
});

test.after.always(async (t) => {
    try {
        const response = await hubClientDelete(`${channelUrl}/${channelName}`);
        t.is(getProp('statusCode', response), 202);
    } catch (ex) {
        t.fail(ex);
    }
});
