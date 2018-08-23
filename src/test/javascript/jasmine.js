const Jasmine = require('jasmine');
const reporter = require('./lib/ddt_reporter');

// const specs = process.argv.slice(2);
const jasmine = new Jasmine();

global.hubDomain = process.env.hubDomain;

jasmine.loadConfig({
    spec_dir: '.',
    spec_files: [process.env.FILES],
    stopSpecOnExpectationFailure: true,
});

jasmine.clearReporters();
jasmine.addReporter(reporter);
jasmine.execute();
// if (specs.length > 0) {
//
// } else {
//     jasmine.execute();
// }
