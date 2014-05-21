frisby = require('frisby');
utils = require('./utils.js');

//todo - gfm - 5/21/14 - might be nice to pull all the config into an object
hubDomain = process.env.hubDomain || 'hub.svc.dev';
replicationDomain = process.env.replicationDomain || 'hub.svc.staging';
runEncrypted = process.env.runEncrypted || false;

// Try to load a local file if it exists. Allows folks to trump default values defined above.
try {
	require('./integration_config_local.js');
}
catch ( err ){}

console.log("hubDomain " + hubDomain);
console.log("replicationDomain " + replicationDomain);
console.log("runEncrypted " + runEncrypted);

hubUrlBase = 'http://' + hubDomain;

channelUrl = hubUrlBase + '/channel';
