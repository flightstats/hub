const rp = require('request-promise-native');
const moment = require('moment');
const {
    fromObjectPath,
    getProp,
    hubClientChannelRefresh,
    hubClientDelete,
    hubClientPut,
    hubClientPostTestItem,
    randomChannelName,
} = require('../lib/helpers');
const {
    getChannelUrl,
} = require('../lib/config');

const channelUrl = getChannelUrl();
const channel = randomChannelName();
const channelResource = `${channelUrl}/${channel}`;
const headers = { 'Content-Type': 'application/json' };
const mutableTime = moment.utc().subtract(3, 'years');
const tag = Math.random().toString().replace(".", "");
const channelBody = {
    mutableTime: mutableTime.format('YYYY-MM-DDTHH:mm:ss.SSS'),
    tags: [tag, "test"],
};
const earliestTime = mutableTime.subtract(2, 'years');
const channelBodyChange = {
    mutableTime: moment(earliestTime).add(1, 'years').format('YYYY-MM-DDTHH:mm:ss.SSS'),
    tags: [tag, "test"],
};
const parameters = "?trace=true&stable=false";
const next7 = earliestTime.subtract(1, 'month').format('/YYYY/MM/DD/HH/mm/ss/SSS') + "/0/next/7" + parameters;
const items = [];
const getFormattedUrl = time =>
    `${channelResource}${time.format('/YYYY/MM/DD/HH/mm/ss/SSS')}`;
/**
 * This should:
 * Create a channel with mutableTime
 * add 3 historical items
 * add 3 live items
 *
 * Query items by direction, verify exclusion
 *
 * Change mutableTime to include one historical item
 * Query items by direction, verify exclusion
 */
describe(__filename, function () {
    beforeAll(async () => {
        const response = await hubClientPut(channelResource, headers, channelBody);
        expect(getProp('statusCode', response)).toEqual(201);
    });

    it(`posts historical items to  ${channel}`, async () => {
        const response = await hubClientPostTestItem(getFormattedUrl(earliestTime));
        items.push(fromObjectPath(['headers', 'location'], response));
        const response1 = await hubClientPostTestItem(getFormattedUrl(earliestTime.add(1, 'years')));
        items.push(fromObjectPath(['headers', 'location'], response1));
        const response2 = await hubClientPostTestItem(getFormattedUrl(earliestTime.add(6, 'months')));
        items.push(fromObjectPath(['headers', 'location'], response2));
    });

    it(`posts live items to ${channel}`, async () => {
        const response = await hubClientPostTestItem(channelResource);
        items.push(fromObjectPath(['headers', 'location'], response));
        const response1 = await hubClientPostTestItem(channelResource);
        items.push(fromObjectPath(['headers', 'location'], response1));
        const response2 = await hubClientPostTestItem(channelResource);
        items.push(fromObjectPath(['headers', 'location'], response2));
    });

    it(`queries next 7 All ${next7}`, async () => {
        try {
            const response = await rp({
                url: `${channelResource}${next7}&epoch=ALL`,
                method: 'GET',
                resolveWithFullResponse: true,
            });
            expect(getProp('statusCode', response)).toEqual(200);
            const body = JSON.parse(getProp('body', response));
            const uris = fromObjectPath(['_links', 'uris'], body);
            expect(uris.length).toBeGreaterThan(0);
            const actual = uris.every((uri, index) => uri === items[index]);
            expect(actual).toEqual(true);
        } catch (ex) {
            console.log('failed with exception: ', ex && ex.message);
            return fail(ex);
        }
    });

    it(`queries next 7 Immutable ${next7}`, async () => {
        try {
            const response = await rp({
                url: `${channelResource}${next7}&epoch=IMMUTABLE`,
                method: 'GET',
                resolveWithFullResponse: true,
            });
            expect(getProp('statusCode', response)).toEqual(200);
            const body = JSON.parse(getProp('body', response));
            const uris = fromObjectPath(['_links', 'uris'], body);
            const expected = items.slice(3);
            expect(uris.length).toEqual(expected.length);
            const actual = uris.every((uri, index) => uri === expected[index]);
            expect(actual).toEqual(true);
        } catch (ex) {
            console.log('failed with exception: ', ex && ex.message);
            return fail(ex);
        }
    });

    it(`queries next 7 Mutable ${next7}`, async () => {
        try {
            const response = await rp({
                url: `${channelResource}${next7}&epoch=MUTABLE`,
                method: 'GET',
                resolveWithFullResponse: true,
            });
            expect(getProp('statusCode', response)).toEqual(200);
            const body = JSON.parse(getProp('body', response));
            const uris = fromObjectPath(['_links', 'uris'], body);
            const expected = items.slice(0, 3);
            expect(uris.length).toEqual(expected.length);
            const actual = uris.every((uri, index) => uri === expected[index]);
            expect(actual).toEqual(true);
        } catch (ex) {
            console.log('failed with exception: ', ex && ex.message);
            return fail(ex);
        }
    }, 3 * 60 * 1000);

    it('updates the mutableTime value', async () => {
        const response = await hubClientPut(channelResource, headers, channelBodyChange);
        expect(getProp('statusCode', response)).toEqual(201);
    });

    it('waits while the channel is refreshed', async () => {
        const response = await hubClientChannelRefresh();
        expect(getProp('statusCode', response)).toEqual(200);
    });

    it(`queries next 7 Immutable after change ${next7}`, async () => {
        try {
            const response = await rp({
                url: `${channelResource}${next7}&epoch=IMMUTABLE`,
                method: 'GET',
                resolveWithFullResponse: true,
            });
            expect(getProp('statusCode', response)).toEqual(200);
            const body = JSON.parse(getProp('body', response));
            const uris = fromObjectPath(['_links', 'uris'], body);
            const expected = items.slice(2);
            expect(uris.length).toEqual(expected.length);
            const actual = uris.every((uri, index) => uri === expected[index]);
            expect(actual).toEqual(true);
        } catch (ex) {
            console.log('failed with exception: ', ex && ex.message);
            return fail(ex);
        }
    }, 3 * 60 * 1000);

    it(`queries next 7 Mutable after change${next7}`, async () => {
        try {
            const response = await rp({
                url: `${channelResource}${next7}&epoch=MUTABLE`,
                method: 'GET',
                resolveWithFullResponse: true,
            });
            expect(getProp('statusCode', response)).toEqual(200);
            const body = JSON.parse(getProp('body', response));
            const uris = fromObjectPath(['_links', 'uris'], body);
            const expected = items.slice(0, 2);
            expect(uris.length).toEqual(expected.length);
            const actual = uris.every((uri, index) => uri === expected[index]);
            expect(actual).toEqual(true);
        } catch (ex) {
            console.log('failed with exception: ', ex && ex.message);
            return fail(ex);
        }
    }, 3 * 60 * 1000);
    it('queries earliest 2 Immutable after change ', async () => {
        try {
            const response = await rp({
                url: `${channelResource}/earliest/2${parameters}&epoch=IMMUTABLE`,
                method: 'GET',
                resolveWithFullResponse: true,
            });
            expect(getProp('statusCode', response)).toEqual(200);
            const body = JSON.parse(getProp('body', response));
            const uris = fromObjectPath(['_links', 'uris'], body);
            const expected = items.slice(2, 4);
            expect(uris.length).toEqual(expected.length);
            const actual = uris.every((uri, index) => uri === expected[index]);
            expect(actual).toEqual(true);
        } catch (ex) {
            console.log('failed with exception: ', ex && ex.message);
            return fail(ex);
        }
    }, 5 * 60 * 1000);

    it('queries earliest 2 Mutable after change ', async () => {
        try {
            const response = await rp({
                url: `${channelResource}/earliest/2${parameters}&epoch=MUTABLE`,
                method: 'GET',
                resolveWithFullResponse: true,
            });
            expect(getProp('statusCode', response)).toEqual(200);
            const body = JSON.parse(getProp('body', response));
            const uris = fromObjectPath(['_links', 'uris'], body);
            const expected = items.slice(0, 2);
            expect(uris.length).toEqual(expected.length);
            const actual = uris.every((uri, index) => uri === expected[index]);
            expect(actual).toEqual(true);
        } catch (ex) {
            console.log('failed with exception: ', ex && ex.message);
            return fail(ex);
        }
    }, 5 * 60 * 1000);

    afterAll(async () => {
        await hubClientDelete(channelResource);
    });
});
