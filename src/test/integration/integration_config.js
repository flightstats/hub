frisby = require('frisby');
utils = require('./utils.js');
ip = require('ip');
var _ = require('lodash');
var reporter = require('./ddt_reporter');

hubDomain = process.env.hubDomain;
satelliteDomain = process.env.satelliteDomain;
runEncrypted = process.env.runEncrypted || false;
integrationTestPath = process.env.integrationTestPath || 'src/test/integration/';
callbackPort = runEncrypted = process.env.callbackPort || 8888;

//this does not report the correct ip address when connected via the vpn
//override ipAddress in integration_config_local.js
ipAddress = ip.address();

// Try to load a local file if it exists. Allows folks to trump default values defined above.
try {
	require('./integration_config_local.js');
}
catch ( err ){}

if (_.startsWith(hubDomain, 'http')) {
    hubUrlBase = hubDomain;
} else {
    hubUrlBase = 'http://' + hubDomain;
}

if (_.startsWith(satelliteDomain, 'http')) {
    satelliteUrl = satelliteDomain;
} else {
    satelliteUrl = 'http://' + satelliteDomain;
}

channelUrl = hubUrlBase + '/channel';
callbackDomain = 'http://' + ipAddress;
stableOffset = 5;

jasmine.getEnv().defaultTimeoutInterval = 60 * 1000;
jasmine.getEnv().reporter.subReporters_.length = 0;
jasmine.getEnv().addReporter(reporter);

// TODO: remove this hacky workaround
// jasmine-node doesn't provide a way to add reporters so I had to hard code it.
// in order for the JUnitReporter to still work I needed to hardcode it also, but only
// when running in Jenkins. This is a short-term solution until I get everything
// migrated to Jasmine 2.0 (which removes the need for jasmine-node).
var useJUnitReporter = process.env.JENKINS_URL !== undefined;
if (useJUnitReporter) {

    // hardcoded values typically provided by the CLI
    var savePath = 'build/reports/integration/';
    var consolidate = true;
    var useDotNotation = true;

    // stolen from jasmine-node's index.js:116-120
    var existsSync = require('fs').existsSync;
    if(!existsSync(savePath)) {
        util.puts('creating junit xml report save path: ' + savePath);
        mkdirp.sync(savePath, "0755");
    }

    jasmine.getEnv().addReporter(new jasmine.JUnitXmlReporter(savePath, consolidate, useDotNotation));
}

console.log("hubDomain " + hubDomain);
console.log("satelliteDomain " + satelliteDomain);
console.log("runEncrypted " + runEncrypted);
console.log("callbackDomain " + callbackDomain);
console.log("default timeout " + jasmine.getEnv().defaultTimeoutInterval + "ms");
