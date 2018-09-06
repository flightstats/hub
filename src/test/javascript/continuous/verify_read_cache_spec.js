const moment = require('moment');
const { getChannelUrl, getHubUrlBase } = require('../lib/config');
const {
    followRedirectIfPresent,
    fromObjectPath,
    getProp,
    hubClientGet,
    randomNumberBetweenInclusive,
} = require('../lib/helpers');

const channelName = 'batch_test_1';
const channelResource = `${getChannelUrl()}/${channelName}`;
const headers = { 'Content-Type': 'application/json' };
let shouldSkipRemainingTests = false;
let spokeTTLMinutes = null;
let urisToChooseFrom = null;
let itemURL = null;
let timeURL = null;
let itemPayload = null;

const getRandomURI = (uris) => {
    let uriIndex = randomNumberBetweenInclusive(0, uris.length - 1);
    return uris[uriIndex];
};

const getTimeURIFromItemURI = (itemURI) => {
    let hashIndex = itemURI.lastIndexOf('/');
    let msIndex = itemURI.lastIndexOf('/', hashIndex - 1);
    return itemURI.slice(0, msIndex);
};

const getUniqueURI = (urisToChooseFrom, ...alreadyUsedURIs) => {
    let uri = getRandomURI(urisToChooseFrom);
    while (alreadyUsedURIs.includes(uri)) {
        console.log('randomly selected uri already tried; skipping', uri);
        uri = getRandomURI(urisToChooseFrom);
    }
    return uri;
};
/*
this test makes a few assumptions:
- there would a channel named "batch_test_1" on the hub you run this against
- that channel has data in it older than spokeTTLMinutes
- (if) we can find an item, not already in the read cache, in 3 attempts
if one of the above assumptions is not true the entire spec is skipped
*/
describe(__filename, () => {
    it(`has a channel named "${channelName}"`, async () => {
        const response = await hubClientGet(channelResource);
        if (getProp('statusCode', response) !== 200) {
            shouldSkipRemainingTests = true;
        }
    });

    it('gets the spokeTTLMinutes for the cluster', async () => {
        if (shouldSkipRemainingTests) return pending();
        const url = `${getHubUrlBase()}/internal/properties`;
        const response = await hubClientGet(url, headers);
        expect(getProp('statusCode', response)).toEqual(200);
        const properties = fromObjectPath(['body', 'properties'], response) || {};
        spokeTTLMinutes = properties['spoke.write.ttlMinutes'];
        console.log('spokeTTLMinutes:', spokeTTLMinutes);
    });

    it('has data old enough to use', async () => {
        if (shouldSkipRemainingTests) return pending();
        expect(spokeTTLMinutes).toBeDefined();
        const timeInThePast = moment().utc().subtract(spokeTTLMinutes + 30, 'minutes');
        const timePath = timeInThePast.format('YYYY/MM/DD/HH/mm');
        const url = `${channelResource}/${timePath}`;
        const response = await hubClientGet(url, headers);
        const urisToChooseFrom = fromObjectPath(['body', '_links', 'uris'], response) || [];
        if (!urisToChooseFrom.length) {
            shouldSkipRemainingTests = true;
            console.log('no data old enough');
        }
    });

    it('tries to get data to use for verification', async () => {
        if (shouldSkipRemainingTests) return pending();
        expect(urisToChooseFrom).toBeDefined();
        expect(Array.isArray(urisToChooseFrom)).toBeTruthy();
        expect(urisToChooseFrom.length).toBeGreaterThan(0);
        itemURL = getRandomURI(urisToChooseFrom);
        timeURL = getTimeURIFromItemURI(itemURL);
        console.log('trying to get usable data (first attempt)');
        const url = `${timeURL}?location=CACHE_READ`;
        const res = await hubClientGet(url, headers);
        const response = followRedirectIfPresent(res, headers);
        const uris = fromObjectPath(['body', '_links', 'uris'], response) || [];
        if (uris.includes(itemURL)) {
            itemURL = getUniqueURI(urisToChooseFrom, itemURL);
            timeURL = getTimeURIFromItemURI(itemURL);
            console.log('trying to get usable data (second attempt)');
            const res2 = await hubClientGet(`${itemURL}?location=CACHE_READ`, headers);
            const response2 = followRedirectIfPresent(res2, headers);
            const uris2 = fromObjectPath(['body', '_links', 'uris'], response2) || [];
            if (uris2.includes(itemURL)) {
                itemURL = getUniqueURI(urisToChooseFrom, itemURL);
                timeURL = getTimeURIFromItemURI(itemURL);
                console.log('trying to get usable data (third attempt)');
                const res3 = await hubClientGet(`${itemURL}?location=CACHE_READ`, headers);
                const response3 = await followRedirectIfPresent(res3, headers);
                const uris3 = fromObjectPath(['body', '_links', 'uris'], response3) || [];
                if (uris3.includes(itemURL)) {
                    shouldSkipRemainingTests = true;
                    console.log('giving up on finding usable data');
                } else {
                    console.log('itemURL:', itemURL);
                    console.log('timeURL:', timeURL);
                }
            } else {
                console.log('itemURL:', itemURL);
                console.log('timeURL:', timeURL);
            }
        } else {
            console.log('itemURL:', itemURL);
            console.log('timeURL:', timeURL);
        }
    });

    it('receives a list without our item from the write cache', async () => {
        if (shouldSkipRemainingTests) pending();
        expect(timeURL).toBeDefined();
        const res = await hubClientGet(`${timeURL}?location=CACHE_WRITE`, headers);
        const response = await followRedirectIfPresent(res, headers);
        const uris = fromObjectPath(['body', '_links', 'uris'], response) || [];
        console.log('uris:', uris);
        expect(uris).not.toContain(itemURL);
    });

    it('receives a list without our item from the read cache', async () => {
        if (shouldSkipRemainingTests) pending();
        expect(timeURL).toBeDefined();
        const res = await hubClientGet(`${timeURL}?location=CACHE_READ`, headers);
        const response = await followRedirectIfPresent(res, headers);
        const uris = fromObjectPath(['body', '_links', 'uris'], response) || [];
        console.log('uris:', uris);
        expect(uris).not.toContain(itemURL);
    });

    it('gets the item from default sources', async () => {
        if (shouldSkipRemainingTests) pending();
        expect(itemURL).toBeDefined();
        const response = await hubClientGet(itemURL);
        expect(getProp('statusCode', response)).toEqual(200);
        itemPayload = getProp('body', response);
    });

    it('receives a list without our item from the write cache still', async () => {
        if (shouldSkipRemainingTests) pending();
        expect(timeURL).toBeDefined();
        const res = await hubClientGet(`${timeURL}?location=CACHE_WRITE`, headers);
        const response = await followRedirectIfPresent(res, headers);
        const uris = fromObjectPath(['body', '_links', 'uris'], response) || [];
        console.log('uris:', uris);
        expect(uris).not.toContain(itemURL);
    });

    it('receives a list with our item from the read cache', async () => {
        if (shouldSkipRemainingTests) pending();
        expect(timeURL).toBeDefined();
        const res = await hubClientGet(`${timeURL}?location=CACHE_READ`, headers);
        const response = await followRedirectIfPresent(res, headers);
        const uris = fromObjectPath(['body', '_links', 'uris'], response) || [];
        console.log('uris:', uris);
        expect(uris).toContain(itemURL);
    });

    it('verifies the item from read cache matches the item from S3', async () => {
        if (shouldSkipRemainingTests) pending();
        expect(itemPayload).toBeDefined();
        const response = await hubClientGet(itemURL);
        expect(getProp('statusCode', response)).toEqual(200);
        expect(getProp('body', response)).toEqual(itemPayload);
    });
});
