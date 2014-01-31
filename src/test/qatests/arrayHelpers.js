/**
 * Created with JetBrains WebStorm.
 * User: gnewcomb
 * Date: 3/5/13
 * Time: 9:23 AM
 * To change this template use File | Settings | File Templates.
 */

var lodash = require('lodash');

var compactArray = function(sourceArray) {
    var compacted = [];

    for (var i = 0; i < sourceArray.length; i += 1) {
        if (i in sourceArray) {
            compacted.push(sourceArray[i]);
        }
    }

    return compacted;
}
module.exports.compactArray = compactArray;

// Case-insensitive and order-agnostic comparison of the contents of two arrays of strings
var strArrayContentsMatch = function(a1, a2) {
    var lower1 = [], lower2 = [];

    a1 = compactArray(a1);
    a2 = compactArray(a2);

    if (a1.length != a2.length) {
        return false;
    }

    // Lower-case-ify contents of both arrays (we know lengths are equal now)
    for (var i = 0; i < a1.length; i += 1) {
        lower1[i] = String(a1[i]).toLowerCase();
        lower2[i] = String(a2[i]).toLowerCase();
    }

    // Each item in lower1 must exist in lower2 or bail
    for (var i = 0; i < lower1.length; i += 1) {
        if (lower2.indexOf(lower1[i]) < 0) {
            return false;
        }
    }

    return true;
}
module.exports.strArrayContentsMatch = strArrayContentsMatch;

// returns array of two arrays: the first is the list of items only in a, the second is those only in b.
// a complete match means both arrays will be empty
var compareArrays = function(a, b) {
    var onlyA = [], onlyB = [];
    onlyA = lodash.difference(a,b);
    onlyB = lodash.difference(b,a);

    return [onlyA, onlyB];
}
exports.compareArrays = compareArrays;