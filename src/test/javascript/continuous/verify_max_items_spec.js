const { getHubUrlBase } = require('../lib/config');
const {
    fromObjectPath,
    getProp,
    hubClientGet,
    hubClientPostTestItem,
    processChunks,
    toChunkArray,
} = require('../lib/helpers');
const verifyMaxItems = `${getHubUrlBase()}/channel/verifyMaxItems`;
const maxItems = 10;
const headers = { 'Content-Type': 'application/json' };
// TODO: I don't think is being used ????
/**
 * This is designed to run once a day.
 * It should:
 *
 * 1 - Create channel
 * 2 - Verify that the channel has the correct number of items in it.
 * * THis will fail the first time you run it.adding
 * 3 - add more items.
 *
 */

describe(__filename, function () {
    it('1 - creates verifyMaxItems channel', async () => {
        const body = { maxItems };
        const res = await hubClientGet(verifyMaxItems, headers, body);
        expect(getProp('statusCode', res)).toBe(200);
        console.log('created', getProp('body', res));
    });

    it('2 - checks for max items', async () => {
        const checkUrl = `${verifyMaxItems}/latest/${(maxItems * 2)}`;
        console.log('check url ', checkUrl);
        const res = await hubClientGet(checkUrl);
        expect(getProp('statusCode', res)).toBe(200);
        const uris = fromObjectPath(['body', '_links', 'uris'], res) || [];
        expect(uris.length).toBe(maxItems);
    });

    it('2 - adds 2 * N items', async () => {
        // timesLimit(n, limit, iterator, [callback])
        const iteratorFunc = async (url) => {
            const res = await hubClientPostTestItem(url);
            // expect(getProp('statusCode', res)).toBe(201);
            console.log('completed adding items');
            return getProp('statusCode', res);
        };
        const times = new Array(2 * maxItems).fill(verifyMaxItems);
        const chunks = toChunkArray(times, 5);
        const responses = await processChunks(chunks, iteratorFunc);
        expect(responses.every(v => v)).toBe(true);
    });
});
