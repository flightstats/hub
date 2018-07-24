const rp = require('request-promise-native');
// const { getProp } = require('./functional');

module.exports.getHubItem = async (uri) => {
    try {
        console.log(`fetching hub item at: ${uri}`);
        const result = await rp(uri, { encoding: null });
        return result || {};
    } catch (ex) {
        console.log('got error ', uri, ex);
        return {};
    }
};

module.exports.createChannel = async (channelName, url, description) => {
    const defaultDescription = description || 'none';
    const defaultUrl = url || channelUrl;
    console.log('channelUrl', defaultUrl);
    console.log(`creating channel ${channelName} for ${defaultDescription}`);
    try {
        const result = await rp({
            method: 'POST',
            url: defaultUrl,
            body: { name: channelName },
            headers: {"Content-Type": "application/json"},
        });
        return result || {};
    } catch (ex) {
        console.log(`get error creating channel for ${defaultDescription}: ${ex}`);
        return {};
    }
};
