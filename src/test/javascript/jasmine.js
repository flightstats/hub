const Jasmine = require('jasmine');
const reporter = require('./lib/ddt_reporter');

const specs = process.argv.slice(2);
const jasmine = new Jasmine();

global.hubDomain = process.env.hubDomain;

jasmine.loadConfig({
    spec_dir: '.',
    spec_files: ['**/*_spec.js'],
    stopSpecOnExpectationFailure: true,
});

jasmine.clearReporters();
jasmine.addReporter(reporter);

if (specs.length > 0) {
    jasmine.execute(specs);
} else {
    jasmine.execute();
}
