/* How to use this for testing:

1. Start backup client on an existing channel -- ensure the root diretory into which it is going to put data is empty.
2. Find uri to latest piece of data in the channel and save it for later.
3. Put some data into the channel.
4. Ensure the backup client has caught up to the data you put in (by specifying the inclusion of seconds in the file/directory
    name, you can see when the backup client has finished creating directories once it's shown no activity for a bit).
5. Get the *next* uri after the one saved in #2 above (this is the first uri that should've been backed up by the backup client).
6. Pass that uri to this tool and run this tool.

 */


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
        actualFiles.push(stat.name.slice(0, 16));
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
            console.log('All expected files found.');
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
