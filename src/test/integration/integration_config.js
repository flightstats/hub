frisby = require('frisby');
utils = require('./utils.js');
ip = require('ip');

hubDomain = process.env.hubDomain || 'hub-v2.svc.dev';
replicationDomain = process.env.replicationDomain || 'hub.svc.staging';
runEncrypted = process.env.runEncrypted || false;
integrationTestPath = process.env.integrationTestPath || 'src/test/integration/';
callbackPort = 8888;
if (runEncrypted) {
    callbackPort = 8899;
}
//todo - gfm - 6/5/14 - this does not report the correct ip address when connected via the vpn
//override ipAddress in integration_config_local.js
ipAddress = ip.address();

// Try to load a local file if it exists. Allows folks to trump default values defined above.
try {
	require('./integration_config_local.js');
}
catch ( err ){}

hubUrlBase = 'http://' + hubDomain;

channelUrl = hubUrlBase + '/channel';
groupUrl = hubUrlBase + '/group';
callbackDomain = 'http://' + ipAddress;
stableOffset = 5;

console.log("hubDomain " + hubDomain);
console.log("replicationDomain " + replicationDomain);
console.log("runEncrypted " + runEncrypted);
console.log("callbackDomain " + callbackDomain);
