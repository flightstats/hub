utils = require('./lib/utils.js');
ip = require('ip');

hubDomain = global.hubDomain || process.env.hubDomain;
runEncrypted = process.env.runEncrypted || false;
callbackPort = runEncrypted = process.env.callbackPort || 8888;

//this does not report the correct ip address when connected via the vpn
//override ipAddress in integration_config_local.js
ipAddress = ip.address();

// Try to load a local file if it exists. Allows folks to trump default values defined above.
try {
	require('./integration_config_local.js');
}
catch ( err ){}

if (hubDomain !== undefined && hubDomain.startsWith('http')) {
    hubUrlBase = hubDomain;
} else {
    hubUrlBase = 'http://' + hubDomain;
}

channelUrl = hubUrlBase + '/channel';
callbackDomain = 'http://' + ipAddress;
stableOffset = 5;

jasmine.DEFAULT_TIMEOUT_INTERVAL = 60 * 1000;

console.log("hubDomain " + hubDomain);
console.log("runEncrypted " + runEncrypted);
console.log("callbackDomain " + callbackDomain);
console.log("default timeout " + jasmine.DEFAULT_TIMEOUT_INTERVAL + "ms");
