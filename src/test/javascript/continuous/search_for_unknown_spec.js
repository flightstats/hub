const {
    followRedirectIfPresent,
    getProp,
    fromObjectPath,
    hubClientGet,
    processChunks,
    toChunkArray,
} = require('../lib/helpers');
const { getHubUrlBase } = require('../lib/config');
const hubUrl = getHubUrlBase();
console.log('hubUrl', hubUrl);
const channelInput = process.env.channels;
console.log('channelInput', channelInput);
const confirmationPercentage = parseFloat(process.env.confirmationPercentage || 95);
console.log('confirmationPercentage', confirmationPercentage);

const MINUTE = 60 * 1000;
let urisToVerify = [];
let channels = ['verifier_test_1', 'verifier_test_2', 'verifier_test_3'];
if (channelInput) {
    channels = channelInput.split(",");
}
/**
 * This should:
 * Attempt to get known existing items from the hub which are outside the Spoke TTL.
 * Log any non-2xx responses as failures.
 *
 */
describe(__filename, function () {
    console.log('channels', channels);
    it('runs day queries ', async () => {
        const iteratorFunc = async (channel) => {
            console.log('get channel', channel);
            const url = `${hubUrl}/channel/${channel}/time/day`;
            const headers = { 'Accept': 'application/json' };

            const response = await hubClientGet(url, headers);
            const response1 = await followRedirectIfPresent(response, headers);
            const statusCode = getProp('statusCode', response1);
            try {
                const body = getProp('body', response1);
                const parse = JSON.parse(body);
                const uris = fromObjectPath(['_links', 'uris'], parse) || [];
                return {
                    statusCode,
                    uris,
                };
            } catch (ex) {
                console.log('error', ex);
                return fail(ex);
            }
        };
        const result = {
            uris: [],
            statusCodes: [],
        };
        const chunks = toChunkArray(channels, 2);
        const resultArray = await processChunks(chunks, iteratorFunc);
        const { uris, statusCodes } = resultArray
            .reduce((accum, current) => {
                const { uris, statusCode } = current;
                return {
                    uris: [...accum.uris, ...uris],
                    statusCodes: [...accum.statusCodes, statusCode],
                };
            }, result);
        expect(statusCodes.every(code => code === 200)).toBe(true);
        const amountToTake = Math.floor((uris.length * confirmationPercentage) / 100);
        console.log(`taking ${confirmationPercentage}% of the ${uris.length} items: `, amountToTake);
        do {
            urisToVerify.push(uris[Math.floor(Math.random() * uris.length)]);
        } while (urisToVerify.length < amountToTake);
    }, 5 * MINUTE);

    it('verifies items', async () => {
        console.log('looking at ', urisToVerify.length, ' items');
        const iteratorFunc = async (uri) => {
            const response = await hubClientGet(uri);
            const statusCode = getProp('statusCode', response);
            if (statusCode !== 200) console.log('statusCode', statusCode);
            return statusCode;
        };
        const chunks = toChunkArray(urisToVerify, 50);
        const statusCodes = await processChunks(chunks, iteratorFunc);
        console.log('statusCodes.length', statusCodes.length);
        expect(statusCodes.every(code => {
            const success = code && code === 200;
            if (!success) console.log('code', code);
            return success;
        })).toBe(true);
    }, 30 * MINUTE);
});
