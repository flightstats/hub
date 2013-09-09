/*
 This allows us to decouple endpoints and configuration from tests.  Barely.
 For now it still requires the server to be running on localhost.  We will
 make this better over time.
 */
frisby = require('frisby');
utils = require('./utils.js');

dataHubUrlBase = 'http://datahub-01.cloud-east.dev:8080';
dataHubProxyUrlBase = 'http://datahub-proxy-01.cloud-east.dev:8080';

// Try to load a local file if it exists. Allows folks to trump default values defined above.
try {
    require('./integration_config_local.js');
}
catch (err) {
}

channelUrl = dataHubUrlBase + '/channel';
channelProxyUrl = dataHubProxyUrlBase + '/channel';
