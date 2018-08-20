const rp = require('request-promise-native');
const { fromObjectPath, getProp } = require('./functional');

const isRedirect = statusCode => !!statusCode &&
    (statusCode >= 300 && statusCode <= 399);

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

const hubClientDelete = async (url, headers = {}) => {
    const formattedHeaders = utils.keysToLowerCase(headers);
    const options = {
        url,
        method: 'DELETE',
        headers: formattedHeaders,
        resolveWithFullResponse: true,
    };

    console.log('DELETE >', url, formattedHeaders);
    try {
        const response = await rp(options);
        const statusCode = getProp('statusCode', response);
        console.log('DELETE <', url, statusCode);
        return response || {};
    } catch (ex) {
        const response = getProp('response', ex) || {};
        console.log(`error in hubClientGet: url:: ${url} ::: ${ex}`);
        const statusCode = getProp('statusCode', response);
        console.log('DELETE <', url, statusCode);
        return response;
    }
};

const hubClientUpdates = async (url, headers = {}, body = '', method) => {
    const formattedHeaders = utils.keysToLowerCase(headers);
    const json = !!formattedHeaders['content-type'] &&
        !!formattedHeaders['content-type'].includes('json');
    const options = {
        method,
        url,
        headers: formattedHeaders,
        body,
        resolveWithFullResponse: true,
        json,
    };
    const bytes = (options.json) ? JSON.stringify(body).length : body.length;
    console.log(`${method} >`, url, headers, bytes);
    try {
        const response = await rp(options);
        const responseBody = getProp('body', response);
        const statusCode = getProp('statusCode', response);
        console.log(`${method} <`, url, statusCode);
        try {
            response.body = JSON.parse(responseBody) || {};
        } catch (error) {
            response.body = responseBody || {};
        }
        return response;
    } catch (ex) {
        const response = getProp('response', ex) || {};
        const statusCode = getProp('statusCode', response);
        console.log(`error in hubClient: url:: ${url} ::: ${ex}`);
        console.log(`${method} <`, url, statusCode);
        return response;
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
        const body = getProp('body', result);
        if (json && body) {
            try {
                result.body = JSON.parse(body) || {};
            } catch (ex) {
                console.log('parsing json error ', ex);
            }
        }
        console.log('GET <', url, getProp('statusCode', result));
        return result || {};
    } catch (ex) {
        const response = getProp('response', ex) || {};
        const statusCode = getProp('statusCode', response);
        if (isRedirect(statusCode)) {
            console.log('<REDIRECT> <', url, statusCode);
            return response;
        }
        console.log(`error in hubClientGet: url:: ${url} ::: ${ex}`);
        console.log('GET <', url, statusCode);
        return response;
    }
};

const hubClientPatch = async (url, headers = {}, body = '') => {
    const response = await hubClientUpdates(url, headers, body, 'PATCH');
    return response;
};

const hubClientPost = async (url, headers = {}, body = '') => {
    const response = await hubClientUpdates(url, headers, body, 'POST');
    return response;
};

const hubClientPut = async (url, headers = {}, body = '') => {
    const response = await hubClientUpdates(url, headers, body, 'PUT');
    return response;
};

const defaultData = JSON.stringify({ data: Date.now() });
const hubClientPostTestItem = async (url, body = defaultData) => {
    const headers = {
        'Content-Type': 'application/json',
        user: 'somebody',
    };
    const response = await hubClientPost(url, headers, body);
    if (getProp('statusCode', response) < 400) {
        console.log(`posted data to: ${fromObjectPath(['headers', 'location'], response)}`);
    }
    return response || {};
};

const followRedirectIfPresent = async (response, headers = {}) => {
    const statusCode = getProp('statusCode', response);
    const location = fromObjectPath(['headers', 'location'], response);
    console.log('statusCode', statusCode);
    const redirectCode = isRedirect(statusCode);
    console.log('location', location);
    if (redirectCode && !!location) {
        const newResponse = await hubClientGet(location, headers);
        return newResponse;
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

const hubClientChannelRefresh = async () => {
    try {
        const response = await rp({
            method: 'GET',
            url: `${global.hubUrlBase}/internal/channel/refresh`,
            resolveWithFullResponse: true,
        });
        console.log('refreshed channels');
        return response || {};
    } catch (ex) {
        console.log('error refreshing hub channels ', ex && ex.message);
        return ex || {};
    }
};

module.exports = {
    createChannel,
    followRedirectIfPresent,
    getHubItem,
    hubClientChannelRefresh,
    hubClientDelete,
    hubClientGet,
    hubClientPatch,
    hubClientPost,
    hubClientPostTestItem,
    hubClientPut,
};
