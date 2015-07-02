frisby = require('frisby');
utils = require('./utils.js');
ip = require('ip');

hubDomain = process.env.hubDomain;
replicationDomain = process.env.replicationDomain;
replicationV2Domain = process.env.replicationV2Domain;
runEncrypted = process.env.runEncrypted || false;
integrationTestPath = process.env.integrationTestPath || 'src/test/integration/';
callbackPort = 8888;
if (runEncrypted) {
    callbackPort = 8899;
}
//this does not report the correct ip address when connected via the vpn
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
alertUrl = hubUrlBase + '/alert';
callbackDomain = 'http://' + ipAddress;
stableOffset = 5;

console.log("hubDomain " + hubDomain);
console.log("replicationDomain " + replicationDomain);
console.log("replicationV2Domain " + replicationV2Domain);
console.log("runEncrypted " + runEncrypted);
console.log("callbackDomain " + callbackDomain);

jasmine.getEnv().defaultTimeoutInterval = 60 * 1000;
