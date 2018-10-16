const { URL } = require('url');
const rp = require('request-promise-native');
const moment = require('moment');
const { fromObjectPath, getProp, itSleeps } = require('./functional');
const { getChannelUrl, getHubUrlBase } = require('../config');

const channelUrl = getChannelUrl();

const isRedirect = statusCode => !!statusCode &&
    (statusCode >= 300 && statusCode <= 399);

const toLowerCase = (str) => {
    let output = '';
    for (let i = 0; i < str.length; ++i) {
        const character = str[i];
        const code = parseInt(character, 36) || character;
        output += code.toString(36);
    }
    return output;
};
const keysToLowerCase = (obj) => {
    const output = {};
    const keys = Object.keys(obj);
    for (let i = 0; i < keys.length; ++i) {
        const originalKey = keys[i];
        const lowerCaseKey = toLowerCase(originalKey);
        output[lowerCaseKey] = obj[originalKey];
    }
    return output;
};

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
    const formattedHeaders = keysToLowerCase(headers);
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
        console.log(`error in hubClientDelete: url:: ${url} ::: ${ex}`);
        const statusCode = getProp('statusCode', response);
        console.log('DELETE <', url, statusCode);
        return response;
    }
};

const hubClientUpdates = async (url, headers = {}, body = '', method) => {
    const formattedHeaders = keysToLowerCase(headers);
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
        console.log(`error in hubClient"${method}": url:: ${url} ::: ${ex}`);
        console.log(`${method} <`, url, statusCode);
        return response;
    }
};

const hubClientGet = async (url, headers = {}, isBinary) => {
    const formattedHeaders = keysToLowerCase(headers);
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
    console.log('redirecting to location: ', location);
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
            url: `${getHubUrlBase()}/internal/channel/refresh`,
            resolveWithFullResponse: true,
        });
        console.log('refreshed channels');
        return response || {};
    } catch (ex) {
        console.log('error refreshing hub channels ', ex && ex.message);
        return ex || {};
    }
};

const hubClientGetUntil = async (url, clause, timeoutMS = 30000, interval = 1000) => {
    try {
        const headers = { 'Content-Type': 'application/json' };
        const timeout = moment().utc().add(timeoutMS, 'ms');
        let error = false;

        const exitOnTimeout = () => {
            let now = moment.utc();
            if (now.isSameOrAfter(timeout)) return true;
            return false;
        };
        let response = {};
        do {
            await itSleeps(1000);
            response = await hubClientGet(url, headers);
            error = exitOnTimeout();
        } while (!clause(response) && !error);
        return response || {};
    } catch (ex) {
        console.log('error in hubClientGetUntil:: ', ex && ex.message);
        return {};
    }
};

const getPort = (path) => {
    const url = new URL(path);
    return url.port;
};

const isClusteredHubNode = async () => {
    const headers = { 'Accept': 'application/json' };
    const url = `${getHubUrlBase()}/internal/properties`;
    const response = await hubClientGet(url, headers);
    const properties = fromObjectPath(['body', 'properties'], response) || {};
    if (properties['hub.type'] === 'aws') return true;
    const servers = fromObjectPath(['body', 'servers'], response) || [];
    const server = fromObjectPath(['body', 'server'], response) || '';
    const indeterminate = !server || !servers.length;
    const single = servers.length === 1 && getPort(servers[0]) === getPort(server);
    return !indeterminate && !single;
};

module.exports = {
    createChannel,
    followRedirectIfPresent,
    getHubItem,
    hubClientGetUntil,
    hubClientChannelRefresh,
    hubClientDelete,
    hubClientGet,
    hubClientPatch,
    hubClientPost,
    hubClientPostTestItem,
    hubClientPut,
    isClusteredHubNode,
};
