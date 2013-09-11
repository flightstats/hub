var walk = require('walk'),
    dhh = require('../src/test/qatests/DH_test_helpers/DHtesthelpers.js'),
    lodash = require('lodash');

function usage() {
    console.log("Usage:", process.argv[0], process.argv[1], "<startUri> <root_backup_dir>")
}

if (process.argv.length != 4) {
    usage();
    process.exit()
}

var startUri = process.argv[2],
    rootDir = process.argv[3],
    actualFiles = [],
    walker = walk.walk(rootDir, { followLinks: false });


walker.on('file', function(root, stat, next) {
    // Add this file to the list of files
    if ('.gz' == stat.name.slice(-3)) {
        actualFiles.push(stat.name.slice(0, stat.name.lastIndexOf('.')));
    }
    next();
});

walker.on('end', function() {
    console.log(actualFiles);

    getExpectedItemNames(function(err, expectedNames) {
        var diff = lodash.difference(expectedNames, actualFiles);
        if (diff.length > 0) {
            console.log('There were '+ diff.length +' expected items not found in the backed up root directory.');
            lodash.forEach(diff, function(val) {
                console.log(val);
            })
        }
        else {
            'All expected files found.';
        }

    })
});

var getExpectedItemNames = function(callback) {
    dhh.getUrisAndDataSinceLocation({'startUri': startUri}, function(err, fulldata) {
        var expectedNames = [];
        lodash.forEach(fulldata, function(val) {
            var uri = val['uri'],
                name = uri.slice(uri.lastIndexOf('/') + 1);
            expectedNames.push(name);

            callback(err, expectedNames);
        })
    })
}
