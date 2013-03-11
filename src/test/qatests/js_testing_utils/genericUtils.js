// Includes only uninherited properties. Returns array of keys.
function getDictKeys(d) {
    var keys = [];

    for (var x in d) {
        if (d.hasOwnProperty(x)) {
            keys.push(x);
        }
    }
    return keys;
}
exports.getDictKeys = getDictKeys;

// Very lightweight (read: weak) comparison that assumes dictionary of k:v structure,
//   where v = non-complex value; no nesting allowed.

function dictCompare(d1, d2) {
    var d1keys, d2keys;

    d1keys = getDictKeys(d1);
    d2keys = getDictKeys(d2);

    if (d1keys.length != d2keys.length) return false;

    for (var i = 0; i < d1keys.length;i += 1) {
        var k = d1keys[i];

        if (!d2.hasOwnProperty(k)) {
            return false;
        }

        if (d1[k] !== d2[k]) {
            return false;
        }
    }

    return true;
}
exports.dictCompare = dictCompare;