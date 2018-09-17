require('../integration_config');
const { getChannelUrl, getHubUrlBase } = require('../lib/config');
const {
    fromObjectPath,
    getProp,
    hubClientGet,
    processChunks,
    toChunkArray,
} = require('../lib/helpers');
const moment = require('moment');
const hubUrl = getHubUrlBase();
console.log(hubUrl);
const MINUTE = 60 * 1000;
const NONE = '1970/01/01/00/00/00/001/none';
const channelUrl = getChannelUrl();
const channelLastUpdated = `${getHubUrlBase()}/internal/zookeeper/ChannelLatestUpdated`;
let zkChannels = [];
let valueResults = [];
const headers = { 'Content-Type': 'application/json' };
const isWithinSpokeWindow = (key) => {
    const oneHourAgo = moment().utc().subtract(1, 'hours');
    const timePart = key.substring(0, key.lastIndexOf('/'));
    const keyTime = moment.utc(timePart, "YYYY/MM/DD/HH/mm/ss/SSS");
    console.log(`key ${key} time ${keyTime.format()} ${oneHourAgo.format()}`);
    return oneHourAgo.isBefore(keyTime);
};
// TODO: I don't think this is being used ???
/**
 * This should :
 * 1 - Get /ChannelLatestUpdated list from the hub
 * 2 - Read values for each channel
 * 3 - Get /latest values for each channel
 * 4 - Compare:
 *      if ZK value is NONE, channel should return 404
 *      if latest is not equal to zk, check zk again & compare
 *      check if ZK value is within Spoke cache
 */

describe(__filename, function () {
    it('1 - loads channels from ZooKeeper cache', async () => {
        console.log('channelLastUpdated', channelLastUpdated);
        const res = await hubClientGet(channelLastUpdated, headers);
        expect(getProp('statusCode', res)).toBe(200);
        zkChannels = fromObjectPath(['body', 'children'], res);
    }, MINUTE);

    it('2 - reads values from ZooKeeper cache', async () => {
        const iteratorFunc = async ({ href: zkChannel }) => {
            console.log('get channel', zkChannel);
            let values = {};
            const res = await hubClientGet(zkChannel, headers);
            const indexOfName = channelLastUpdated.length + 1;
            const endIndex = zkChannel.indexOf('?', indexOfName) || zkChannel.length;
            const name = zkChannel.substring(indexOfName, endIndex);
            const isNotTestChannel = name.substring(0, 4).toLowerCase() !== 'test';
            if (isNotTestChannel) {
                values = {
                    name,
                    zkKey: fromObjectPath(['body', 'data', 'string'], res),
                    stats: fromObjectPath(['body', 'stats'], res),
                    zkChannel,
                };
                console.log('zk value', name, values.zkKey);
            }
            return values;
        };
        const chunks = toChunkArray(zkChannels, 10);
        const results = await processChunks(chunks, iteratorFunc);
        valueResults = results.filter(item => !!Object.keys(item).length);
    }, MINUTE);

    it('3 - gets /latest', async () => {
        const iteratorFunc = async (item) => {
            const { name } = item;
            console.log('get latest ', name);
            const url = `${channelUrl}/${name}/latest?trace=true`;
            const res = await hubClientGet(url, headers);
            const statusCode = getProp('statusCode', res);
            if (statusCode === 404) {
                item.empty = true;
            } else if (statusCode === 303) {
                const location = fromObjectPath(['headers', 'location'], res);
                const strLength = channelUrl.length + name.length + 2;
                const latestKey = location.substring(strLength);
                console.log('latestKey ', latestKey, location);
                item.latestKey = latestKey;
            } else {
                console.log('unexpected result');
            }
            return item;
        };
        const chunks = toChunkArray(valueResults, 10);
        const results = await processChunks(chunks, iteratorFunc);
        valueResults = results.filter(item => item.empty || !!item.latestKey);
    }, MINUTE);

    it('4 - verifies', async () => {
        const iteratorFunc = async (item) => {
            const { empty, name, latestKey, zkKey, zkChannel } = item;
            if (empty) {
                const res = await hubClientGet(zkChannel, headers);
                const str = fromObjectPath(['body', 'data', 'string'], res);
                console.log('comparing to NONE ', name, str);
                return str === NONE;
            } else {
                if (latestKey !== zkKey) {
                    console.log('doing comparison on ', item);
                    const res2 = await hubClientGet(zkChannel, headers);
                    if (getProp('statusCode', res2) === 404) {
                        console.log('missing zk value for ' + name);
                        return isWithinSpokeWindow(latestKey);
                    } else {
                        const dataStr = fromObjectPath(['body', 'data', 'string'], res2);
                        return dataStr === latestKey;
                    }
                } else {
                    const withinSpokeWindow = isWithinSpokeWindow(latestKey);
                    if (withinSpokeWindow) {
                        console.log('comparing ', item);
                    }
                    return latestKey === zkKey && !withinSpokeWindow;
                }
            }
        };
        const chunks = toChunkArray(valueResults, 10);
        const results = await processChunks(chunks, iteratorFunc);
        expect(results.every(v => v)).toBe(true);
    }, MINUTE);
});
