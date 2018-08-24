require('../integration_config');
const {
    followRedirectIfPresent,
    getProp,
    fromObjectPath,
    hubClientGet,
} = require('../lib/helpers');
const { getHubUrlBase } = require('../lib/config');
const hubUrl = getHubUrlBase();
console.log('hubUrl', hubUrl);
const channelInput = process.env.channels;
console.log('channelInput', channelInput);
const errorRate = parseFloat(process.env.errorRate || 0.95);
console.log('errorRate', errorRate);

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
        const twoChannels = channels.slice(0, 2);
        const result = {
            uris: [],
            statusCodes: [],
        };
        const resultArray = await Promise.all(twoChannels.map(channel => iteratorFunc(channel)));
        const { uris, statusCodes } = resultArray
            .reduce((accum, current) => {
                const { uris, statusCode } = current;
                return {
                    uris: [...accum.uris, ...uris],
                    statusCodes: [...accum.statusCodes, statusCode],
                };
            }, result);
        expect(statusCodes.every(code => code === 200)).toBe(true);
        const amountToTake = Math.floor((uris.length * errorRate) / 100);
        console.log(`taking ${errorRate}% of the ${uris.length} items: `, amountToTake);
        do {
            urisToVerify.push(uris[Math.floor(Math.random() * uris.length)]);
        } while (urisToVerify.length < amountToTake);
    }, 5 * MINUTE);

    const toChunkArray = (arr) => {
        const newArray = [];
        let amountToSplice = 50;
        do {
            amountToSplice = arr.length >= 50 ? 50 : arr.length;
            console.log('amountToSplice', amountToSplice);
            newArray.push(arr.splice(0, amountToSplice));
        } while (amountToSplice > 0);
        return newArray;
    };

    it('verifies items', async () => {
        console.log('looking at ', urisToVerify.length, ' items');
        const iteratorFunc = async (uri) => {
            const response = await hubClientGet(uri);
            const statusCode = getProp('statusCode', response);
            if (statusCode !== 200) console.log('statusCode', statusCode);
            return statusCode;
        };
        const processChunks = async (chunks) => {
            const totalCodes = [];
            for (const chunk of chunks) {
                const statusCodes = await Promise.all(chunk.map(uri => iteratorFunc(uri)));
                totalCodes.push(...statusCodes);
            };
            return totalCodes;
        };
        const chunks = toChunkArray(urisToVerify);
        const statusCodes = await processChunks(chunks);
        console.log('statusCodes.length', statusCodes.length);
        expect(statusCodes.every(code => {
            const success = code && code === 200;
            if (!success) console.log('code', code);
            return success;
        })).toBe(true);
    }, 30 * MINUTE);
});
