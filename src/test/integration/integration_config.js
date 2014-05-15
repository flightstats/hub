/*
 This allows us to decouple endpoints and configuration from tests.  Barely.
 For now it still requires the server to be running on localhost.  We will
 make this better over time.
 */
frisby = require('frisby');
utils = require('./utils.js');

//hubDomain = 'localhost:9080';
hubDomain = 'hub.svc.dev';
//This presumes you have MemoryHubMain running locally
remoteDomain = 'hub.svc.staging';
runEncrypted = process.env.runEncrypted || false;

// Try to load a local file if it exists. Allows folks to trump default values defined above.
try {
	require('./integration_config_local.js');
}
catch ( err ){}

hubUrlBase = 'http://' + hubDomain;

channelUrl = hubUrlBase + '/channel';
