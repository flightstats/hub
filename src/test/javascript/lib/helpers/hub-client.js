const rp = require('request-promise-native');
// const { getProp } = require('./functional');

module.exports.getHubItem = async (url) => {
    try {
        console.log(`fetching hub item at: ${url}`);
        const result = await rp({
            url,
            encoding: null,
            resolveWithFullResponse: true,
        });
        return result || {};
    } catch (ex) {
        console.log('got error ', url, ex);
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
            resolveWithFullResponse: true,
            url: defaultUrl,
            body: { name: channelName },
            headers: {"Content-Type": "application/json"},
            json: true,
        });
        return result || {};
    } catch (ex) {
        console.log(`get error creating channel for ${defaultDescription}: ${ex}`);
        return {};
    }
};
