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

module.exports = {

    passed: 0,
    failed: 0,
    skipped: 0,

    failures: [],

    jasmineStarted: function (info) {
        var message = 'Executing ' + info.totalSpecsDefined + ' specs';
        console.log('\n' + ANSI.BOLD + ANSI.MAGENTA + message + ANSI.OFF);
    },

    suiteStarted: function (info) {
        var indexOfFilename = info.description.lastIndexOf('/') + 1;
        var filename = info.description.slice(indexOfFilename);
        var line = new Array(50).join('-');
        console.log('\n');
        console.log(line);
        console.log(ANSI.BOLD + '  ' + filename + ANSI.OFF);
        console.log(line);
    },

    specStarted: function (info) {
        console.log('\n' + ANSI.BOLD + '> ' + ANSI.BLUE + info.description + ANSI.OFF);
    },

    specDone: function (info) {
        var status = info.status.toUpperCase();
        switch (status) {

            case 'PASSED':
                console.log(ANSI.BOLD + '< ' + ANSI.GREEN + status + ANSI.OFF);
                this.passed++;
                break;

            case 'FAILED':
                console.log(ANSI.BOLD + '< ' + ANSI.RED + status + ANSI.OFF);
                this.failed++;
                for (var i = 0; i < info.failedExpectations.length; ++i) {
                    var stackTrace = info.failedExpectations[i].stack;

                    if (stackTrace === undefined) {
                        console.log('An error occured but a stack trace couldn\'t be found');
                        console.log('DEBUG INFO:', item);
                        exit(1);
                    }

                    console.log(ANSI.RED + stackTrace + ANSI.OFF);

                    this.failures.push({
                        filename: info.fullName.split(' ')[0],
                        stackTrace: stackTrace
                    });
                }
                break;

            case 'DISABLED':
            case 'PENDING':
                this.skipped++;
                console.log(ANSI.BOLD + '< ' + ANSI.YELLOW + status + ANSI.OFF);
                break;

            default:
                console.log(ANSI.BOLD + '< ' + ANSI.CYAN + status + ANSI.OFF);
        }
    },

    suiteDone: function (info) {
        // don't output anything
    },

    jasmineDone: function (info) {
        if (this.failed) {
            var twentyDashes = new Array(20).join('-');
            console.log('\n' + twentyDashes + ' FAILURE SUMMARY ' + twentyDashes);
            this.failures.forEach(function (failure) {
                console.log('\n');
                console.log(ANSI.BOLD + ANSI.RED + failure.filename + ANSI.OFF);
                console.log(ANSI.RED + failure.stackTrace + ANSI.OFF);
            });
        }

        console.log('\n' + new Array(50).join('-') + '\n');
        console.log(ANSI.GREEN + 'PASSED: ' + ANSI.BOLD + this.passed + ANSI.OFF);
        console.log(ANSI.RED + 'FAILED: ' + ANSI.BOLD + this.failed + ANSI.OFF);
        console.log(ANSI.YELLOW + 'SKIPPED: ' + ANSI.BOLD + this.skipped + ANSI.OFF + '\n');
    }

};
