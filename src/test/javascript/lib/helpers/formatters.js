const toLowerCase = (str) => {
    let output = '';
    for (var i = 0; i < str.length; ++i) {
        const character = str[i];
        const code = parseInt(character, 36) || character;
        output += code.toString(36);
    }
    return output;
};
const keysToLowerCase = (obj) => Object.keys(obj)
    .reduce((accum, key) => {
        accum[key] = toLowerCase(obj[key]);
        return accum;
    }, {});

module.exports = {
    keysToLowerCase,
    toLowerCase,
};
