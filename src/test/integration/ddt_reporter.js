const ANSI = {
    OFF: '\u001b[0m',
    BOLD: '\u001b[1m',
    BLINK: '\u001b[5m',
    BLACK: '\u001b[30m',
    RED: '\u001b[31m',
    GREEN: '\u001b[32m',
    YELLOW: '\u001b[33m',
    BLUE: '\u001b[34m',
    MAGENTA: '\u001b[35m',
    CYAN: '\u001b[36m',
    WHITE: '\u001b[37m'
};

module.exports = function (config) {

    this.callback = config.onComplete || false;

    this.passed = 0;
    this.failed = 0;
    this.disabled = 0;

    this.reportRunnerStarting = function (info) {
        console.log('\n' + ANSI.BOLD + ANSI.MAGENTA + 'Executing tests...' + ANSI.OFF);
    };

    this.reportSpecStarting = function (info) {
        console.log('\n' + ANSI.BOLD + '> ' + ANSI.BLUE + info.description + ANSI.OFF);
    };

    this.reportSpecResults = function (info) {
        var results = info.results();
        var status;
        if (results.skipped) {
            status = 'DISABLED';
        } else if (results.passed()) {
            status = 'PASSED';
        } else {
            status = 'FAILED';
        }

        switch (status) {

            case 'PASSED':
                console.log(ANSI.BOLD + '< ' + ANSI.GREEN + status + ANSI.OFF);
                reporter.passed++;
                break;

            case 'FAILED':
                console.log(ANSI.BOLD + '< ' + ANSI.RED + status + ANSI.OFF);
                reporter.failed++;
                results.items_.forEach(function (item) {
                    if (!item.passed_) {
                        console.log(ANSI.RED + item.trace.stack + ANSI.OFF);
                    }
                });
                break;

            case 'DISABLED':
                reporter.disabled++;
                console.log(ANSI.BOLD + '< ' + ANSI.YELLOW + status + ANSI.OFF);
                break;

            default:
                console.log(ANSI.BOLD + '< ' + ANSI.CYAN + status + ANSI.OFF);
        }
    };

    this.reportSuiteResults = function (info) {
        // don't output anything
    };

    this.reportRunnerResults = function (runner) {
        console.log('\n' + new Array(50).join('-') + '\n');
        console.log(ANSI.GREEN + 'PASSED: ' + ANSI.BOLD + reporter.passed + ANSI.OFF);
        console.log(ANSI.RED + 'FAILED: ' + ANSI.BOLD + reporter.failed + ANSI.OFF);
        console.log(ANSI.YELLOW + 'DISABLED: ' + ANSI.BOLD + reporter.disabled + ANSI.OFF);

        if (this.callback) this.callback(runner);
    };

};
