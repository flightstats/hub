const { getHubUrlBase } = require('../lib/config');
const {
    followRedirectIfPresent,
    fromObjectPath,
    getProp,
    hubClientGet,
    processChunks,
    toChunkArray,
    randomItemsFromArrayByPercentage,
} = require('../lib/helpers');

const hubUrl = getHubUrlBase();
console.log(hubUrl);
// contains links to destination channels
const replicatedChannelUrls = [];
// uses key of replicationSource to channel body
const replicatedChannels = {};
const MINUTE = 60 * 1000;
const validReplicatedChannelUrls = [];
const headers = { 'Content-Type': 'application/json' };
const channels = {};
const itemsToVerify = [];
const logDifference = (source, destination) => {
    if ((!source || !source.length) ||
        (!destination || !destination.length)) {
        console.log('source ', source);
        console.log('destination', destination);
        return false;
    }
    if (source.length + destination.length > 10000) {
        console.log("too many items to compare ", source.length, destination.length);
        return false;
    }
    const sourceItems = mapContentKey(source, source[0].split('/')[4]);
    const destinationItems = mapContentKey(destination, destination[0].split('/')[4]);
    console.log('missing from source:', sourceItems.filter(sourceItem => !(sourceItem in destinationItems)));
    console.log('missing from destination:', destinationItems.filter(destinationItem => !(destinationItem in sourceItems)));
};

const mapContentKey = (uris, channel) =>
    uris.map((value) =>
        getContentKey(value, channel));

const getContentKey = (uri, channel) =>
    uri ? uri.substring(uri.lastIndexOf(channel) + channel.length) : '';

const getItem = async (uri) => {
    const response = await hubClientGet(uri);
    const statusCode = getProp('statusCode', response);
    if (statusCode !== 200) {
        console.log('wrong status ', uri, statusCode);
    }
    return getProp('body', response);
};

describe(__filename, function () {
    it(`loads ${hubUrl} replicated channels`, async () => {
        const url = `${hubUrl}/tag/replicated`;
        const res = await hubClientGet(url, headers);
        const channels = fromObjectPath(['body', '_links', 'channels'], res) || [];
        console.log('found replicated channels', channels);
        channels.forEach(channel => {
            if (channel.name.substring(0, 4).toLowerCase() !== 'test') {
                const channelLink = getProp('href', channel);
                console.log('adding channel ', channelLink);
                replicatedChannelUrls.push(channelLink);
                replicatedChannels[channelLink] = channel;
            } else {
                console.log('excluding channel ', channel.name);
            }
        });
        expect(replicatedChannelUrls.length).not.toBe(0);
    });

    it('gets replication sources ', async () => {
        const iteratorFunc = async (channel, callback) => {
            console.log('get channel', channel);
            const res = await hubClientGet(channel, headers);
            const url = fromObjectPath(['body', 'replicationSource'], res);
            const res2 = await hubClientGet(url, headers);
            const url2 = fromObjectPath(['body', 'replicationSource'], res2);
            if (getProp('statusCode', res2) >= 400) {
                console.log('channel is missing remote source ', channel, url2);
            } else {
                console.log('pushing channel', channel);
                const error = getProp('error', res2);
                if (error) return fail(error);
                return { url, channel };
            }
        };
        const chunks = toChunkArray(replicatedChannelUrls, 20);
        const results = await processChunks(chunks, iteratorFunc);
        expect(results).toBeDefined();
        results.forEach(({ url, channel }) => {
            if (url && channel) {
                validReplicatedChannelUrls.push(channel);
                replicatedChannels[channel].replicationSource = url;
            }
        });
    }, MINUTE);

    it('gets lists of replicated items', async () => {
        const iteratorFunc = async (channel) => {
            const url = `${channel}/time/hour?stable=false&trace=true`;
            const response = await hubClientGet(url, headers);
            const res = await followRedirectIfPresent(response, headers);
            const prevLink = fromObjectPath(['body', '_links', 'previous', 'href'], res);
            const res2 = await hubClientGet(prevLink, headers);
            const uris = fromObjectPath(['body', '_links', 'uris'], res2) || [];
            return { uris, channel };
        };
        const chunks = toChunkArray(validReplicatedChannelUrls, 20);
        const results = await processChunks(chunks, iteratorFunc);
        expect(results).toBeTruthy();
        results.forEach(({ uris, channel }) => {
            if (uris && channel) {
                channels[channel] = uris;
                console.log('found dest second ', channels[channel][0]);
            }
        });
    }, 5 * MINUTE);

    it('verifies number of replicated items', async () => {
        const iteratorFunc = async (channel, callback) => {
            const currentChannel = replicatedChannels[channel];
            const source = getProp('replicationSource', currentChannel);
            const response = await hubClientGet(`${source}/time/hour?stable=false`, headers);
            const res = await followRedirectIfPresent(response, headers);
            const prevLink = fromObjectPath(['body', '_links', 'previous', 'href'], res);
            const res2 = await hubClientGet(prevLink, headers);
            const uris = fromObjectPath(['body', '_links', 'uris'], res2) || [];
            if (uris.length !== channels[channel].length) {
                console.log('unequal lengths ', channel, uris.length, channels[channel].length);
                logDifference(uris, channels[channel]);
            }
            return channels[channel].length === uris.length;
        };
        const chunks = toChunkArray(validReplicatedChannelUrls, 20);
        const results = await processChunks(chunks, iteratorFunc);
        expect(results.every(val => val)).toBe(true);
    });

    it('select some random items for content verification ', () => {
        const workableChannels = Object.keys(channels)
            .filter(channel => !(channel.toLowerCase().includes('large_test')));
        const confirmationPercentage = 99;
        const randoms = randomItemsFromArrayByPercentage(workableChannels, confirmationPercentage);
        const formatted = randoms.reduce((accum, key) => {
            if (channels[key] && channels[key].length) {
                const items = channels[key]
                    .map(uri => ({ name: key, uri, contentKey: getContentKey(uri, key) }));
                accum.push(...items);
            }
            return accum;
        }, []);
        itemsToVerify.push(...formatted);
    }, MINUTE);

    it('compares replicated items to source items', async () => {
        const iteratorFunc = async (item) => {
            const parallelArray = [
                getItem(`${replicatedChannels[item.name].replicationSource}${item.contentKey}`),
                getItem(item.uri),
            ];
            const [itemA, itemB] = await Promise.all(parallelArray);
            if (itemA && itemB) {
                const itemALength = itemA.length;
                const itemBLength = itemB.length;
                if (itemBLength !== itemALength) {
                    console.log('wrong length for item ', item.uri, ' expected ', itemALength, ' found ', itemBLength);
                    return false;
                }
            }
            return !!itemA && !!itemB;
        };
        const chunks = toChunkArray(itemsToVerify, 50);
        const responses = await processChunks(chunks, iteratorFunc);
        expect(responses.every(v => v)).toBe(true);
    }, 30 * MINUTE);
});
