/*
 This allows us to decouple endpoints and configuration from tests.  Barely.
 For now it still requires the server to be running on localhost.  We will
 make this better over time.
 */
frisby = require('frisby');
utils = require('./utils.js');

hubDomain = 'localhost:9080';
hubUrlBase = 'http://' + hubDomain;
//This presumes you have MemoryHubMain running locally
remoteDomain = 'localhost:9999';

// Try to load a local file if it exists. Allows folks to trump default values defined above.
try {
	require('./integration_config_local.js');
}
catch ( err ){}

channelUrl = hubUrlBase + '/channel';
