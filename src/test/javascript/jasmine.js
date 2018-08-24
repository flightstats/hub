const Jasmine = require('jasmine');
const reporter = require('./lib/ddt_reporter');
const jasmine = new Jasmine();

global.hubDomain = process.env.hubDomain;
const specs = process.argv[2] || 'integration/*_spec.js';
jasmine.loadConfig({
    spec_dir: '.',
    spec_files: [specs],
    stopSpecOnExpectationFailure: true,
});

jasmine.clearReporters();
jasmine.addReporter(reporter);
jasmine.execute();
