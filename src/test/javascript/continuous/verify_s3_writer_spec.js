require('../integration_config');
const moment = require('moment');
const { getHubUrlBase } = require('../lib/config');
const {
    fromObjectPath,
    getProp,
    hubClientGet,
    processChunks,
    toChunkArray,
} = require('../lib/helpers');
const hubUrl = getHubUrlBase();
console.log(hubUrl);

const timeout = 5 * 60 * 1000;
const minuteFormat = '/YYYY/MM/DD/HH/mm';
const startOffset = parseInt(process.env.startOffset) || 29;
const endOffset = parseInt(process.env.endOffset) || 40;
const testPercent = parseInt(process.env.testPercent) || 10;
let channels = [];
const channelTimes = [];
const headers = { 'Content-Type': 'application/json' };
const addUrls = (rootUrl, type) => {
    channelTimes.push({
        source: `${rootUrl}?location=CACHE&trace=true`,
        compare: `${rootUrl}?location=LONG_TERM_${type}&trace=true`,
    });
};
/**
 * Usage:
 * jasmine-node --forceexit --captureExceptions --config hubUrl hub --config startOffset 48 verify_s3_writer_spec.js
 *
 * This should load all the channels in the hub.
 * For each channel, verify that the items in S3 match the items in Spoke
 * testPercent is used to limit the cost of all the querys
 */
describe(__filename, function () {
    it('loads channels', async () => {
        const res = await hubClientGet(`${hubUrl}/channel`, headers);
        const allChannels = fromObjectPath(['body', '_links', 'channels'], res);
        const noTestInTheName = allChannels.filter(channel =>
            (channel.name.substring(0, 4).toLowerCase() !== 'test'));
        channels.push(...noTestInTheName);
    }, timeout);

    it('loads channel data', async () => {
        const iteratorFunc = async (channel) => {
            console.log('calling', channel);
            const res = await hubClientGet(channel.href, headers);
            channel.storage = fromObjectPath(['body', 'storage'], res);
            channel.start = moment.utc();
            const historical = fromObjectPath(['body', 'historical'], res);
            if (historical) channel.history = true;
            return channel;
        };
        const chunks = toChunkArray(channels, 10);
        const responses = await processChunks(chunks, iteratorFunc);
        channels = responses;
    }, timeout);

    it('loads historical channel data', async () => {
        console.log('channels', channels);
        const historicalChannels = channels.filter(channel => getProp('history', channel));
        const nonHistoricals = channels.filter(channel => !getProp('history', channel));
        const chunks = toChunkArray(historicalChannels, 10);
        const iteratorFunc = async (channel) => {
            console.log('history check ', channel);
            const url = `${channel.href}/latest`;
            const res = await hubClientGet(url, headers);
            const location = fromObjectPath(['header', 'location'], res);
            if (location) {
                const name = getProp('name', channel) || '';
                const lastSlash = location.lastIndexOf(name);
                const substring = location.substring(lastSlash + name.length + 1).substring(0, 20);
                console.log('history', substring);
                channel.start = moment(substring, "YYYY/MM/DD/HH/mm/ss");
            }
            return channel;
        };
        const responses = await processChunks(chunks, iteratorFunc);
        channels = [...nonHistoricals, ...responses];
    }, timeout);

    it('cross product of channels and times', () => {
        console.log('now', moment.utc().format(minuteFormat));
        console.log('startOffset', startOffset);
        console.log('endOffset', endOffset);
        for (var i = startOffset; i <= endOffset; i++) {
            channels.forEach(function (channel) {
                const start = channel.start.subtract(i, 'minutes');
                const formatted = start.format(minuteFormat);
                const name = getProp('name', channel);
                if (name.toLowerCase().startsWith('test') ||
                    name.startsWith('verifyMaxItems') ||
                    Math.random() * 100 > testPercent) {
                    // do nothing
                } else {
                    const href = getProp('href', channel);
                    const storage = getProp('storage', channel);
                    const rootUrl = `${href}${formatted}`;
                    if (storage === 'BOTH') {
                        addUrls(rootUrl, 'SINGLE');
                        addUrls(rootUrl, 'BATCH');
                    } else {
                        addUrls(rootUrl, storage);
                    }
                }
            });
        }
    }, timeout);

    it('compares query results', async () => {
        const iteratorFunc = async (channelTime, callback) => {
            console.log('calling', channelTime);
            let headers = {'Accept': 'application/json'};
            const source = getProp('source', channelTime);
            const compare = getProp('compare', channelTime);
            const asyncGet = async (url) => {
                const response = await hubClientGet(url, headers);
                try {
                    const parsed = JSON.parse(getProp('body', response));
                    return fromObjectPath(['_links', 'uris'], parsed) || [];
                } catch (ex) {
                    return fromObjectPath(['body', '_links', 'uris'], response) || [];
                }
            };
            const parallels = [
                asyncGet(source),
                asyncGet(compare),
            ];
            const [itemA, itemB] = await Promise.all(parallels);
            const expected = itemA && itemA.length;
            const actual = itemB && itemB.length;
            if (expected > actual) {
                console.log('failed ' + channelTime.compare + ' source=' + expected + ' compare=' + actual);
            } else {
                console.log('completed ' + channelTime.compare + ' with ' + expected);
            }
            return actual >= expected;
        };
        const chunks = toChunkArray(channelTimes, 5);
        const responses = await processChunks(chunks, iteratorFunc);
        expect(responses.every(v => v)).toBe(true);
    }, 2 * timeout);
});
