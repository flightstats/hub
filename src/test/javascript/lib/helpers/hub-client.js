const rp = require('request-promise-native');
const { fromObjectPath, getProp } = require('./functional');

const createChannel = async (channelName, url, description) => {
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

const hubClientGet = async (url, headers = {}, isBinary) => {
    const formattedHeaders = utils.keysToLowerCase(headers);
    const json = !!formattedHeaders['content-type'] &&
        !!formattedHeaders['content-type'].includes('json');
    const options = {
        method: 'GET',
        url,
        headers: formattedHeaders,
        followRedirect: false,
        resolveWithFullResponse: true,
        json,
    };

    if (isBinary) {
        options.encoding = null;
    }

    try {
        console.log('GET >', url, headers || {});
        const result = await rp(options);
        if (json) {
            try {
                result.body = JSON.parse(getProp('body', result)) || {};
            } catch (ex) {
                console.log('parsing json error ', ex);
            }
        }
        console.log('GET <', url, getProp('statusCode', result));
        return result || {};
    } catch (ex) {
        console.log(`error in hubClientGet: url:: ${url} ::: ${ex}`);
        const response = getProp('response', ex) || {};
        console.log('GET <', url, getProp('statusCode', response.status));
        return response;
    }
};

const followRedirectIfPresent = async (response) => {
    const statusCode = getProp('statusCode', response);
    const location = fromObjectPath(['headers', 'location'], response);
    const redirectCode = statusCode >= 300 && statusCode <= 399;
    if (redirectCode && !!location) {
        await hubClientGet(location);
    } else {
        return response;
    }
};

const getHubItem = async (url) => {
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

module.exports = {
    createChannel,
    followRedirectIfPresent,
    getHubItem,
    hubClientGet,
};
