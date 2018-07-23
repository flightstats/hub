const axios = require('axios');
// const { getProp } = require('./functional');

module.exports.getHubItem = async (uri) => {
    try {
        console.log(`fetching hub item at: ${uri}`);
        const result = await axios.get(uri, { responseEncoding: null });
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
        const result = await axios({
            method: 'POST',
            url: defaultUrl,
            data: { name: channelName },
            headers: {"Content-Type": "application/json"},
        });
        return result || {};
    } catch (ex) {
        console.log(`get error creating channel for ${defaultDescription}: ${ex}`);
        return {};
    }
};
