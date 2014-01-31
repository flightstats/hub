/**
 * Created with JetBrains WebStorm.
 * User: gnewcomb
 * Date: 2/14/13
 * Time: 2:13 PM
 * To change this template use File | Settings | File Templates.
 */

var ah = require('./arrayHelpers.js');


// returns 0 through (hi - 1)
function randomNum(hi){
    return Math.floor(Math.random()*hi);
}
module.exports.randomNum = randomNum;

function randomChar(){
    // valid chars = 32 to 126 and 160 to 255:
    var index = randomNum(191) + 1 + 31;    // 1 for randomNum func and 31 for min legal value
    if (index > 126) {
        index += 33;
    }

    return String.fromCharCode(index);
}
module.exports.randomChar = randomChar;

function limitedRandomChar() {
    //var allowedChars = 'abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789.-_';
    var allowedChars = 'abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789';
    return allowedChars[randomNum(allowedChars.length)];
}
module.exports.limitedRandomChar = limitedRandomChar;

// ASCII 161 - 447 only
function extendedCharOnly() {
    return String.fromCharCode(160 + randomNum(288));
}
module.exports.extendedCharOnly = extendedCharOnly;

// returns a legal email address that includes at least one of each type of character
function randomEmail() {
    var localName, domain;

    function localChars() {
        var allowed = 'abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789!#$%&\'*+_/=?^-`{|}~';
        return allowed[randomNum(allowed.length)];
    };

    function domainChars() {
        var allowed = 'abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-';
        return allowed[randomNum(allowed.length)];
    }

    localName = randomString(5, localChars) +'.'+ 'aB4!#$%&\'*+_/=?^-`{|}~';
    domain = randomString(5, domainChars) + '-'+ randomString(4, domainChars) +'.'+ randomString(3, domainChars);

    return localName +'@'+ domain;
}
module.exports.randomEmail = randomEmail;

// simulateParagraphs: if true, there is a small chance of responding with '\r\n'. This causes problems when trying to
//      get a string of an exact length, as that means this function can return TWO characters in a call instead of one,
//      so by default this param is false.
function simulatedTextChar(res, simulateParagraphs) {
    var res = randomNum(100) + 1;
    var l, poss = '';

    if ((typeof simulateParagraphs != 'undefined') && (simulateParagraphs)) {

        // last minute hack for paragraphs, should rewrite:
        var paragraphRes = randomNum(200) + 1;
        if (200 == paragraphRes) {
            return "\r\n";
        }
    }

    if (res < 69) {
        poss = 'abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ';
        l = poss[randomNum(poss.length)];
    }
    else if (res < 81) {
        l = ' ';
    }
    else if (res < 88 ) {
        l = ',';
    }
    else if (res < 92) {
        poss = '.!?';
        l = poss[randomNum(poss.length)];
    }
    else if (res < 97) {
        l = ';';
    }
    else if (res < 99) {
        poss = '0123456789';
        l = poss[randomNum(poss.length)];
    }
    else if (res < 101) {
        poss ='\'"';
        l = poss[randomNum(poss.length)];
    }
    else throw new Error('Invalid number (should be 1-100): '+ res);

    //return l +': '+ res;
    return l;
}
module.exports.simulatedTextChar = simulatedTextChar;

// Updated to ensure that the first and last characters are not whitespace
function randomString(requestedLength, characterFunction){
    if (requestedLength == 0) {
        requestedLength = 1;
    }

    var str = "",
        myCharFunc = (arguments.length > 1 ? characterFunction : randomChar);

    /*
    for(var i = 0; i < length; i = i + 1){
        str += myCharFunc();
    }
    */

    while (str.length < requestedLength) {
        str += myCharFunc();
        str = str.trim();
    }

    return str;
}
module.exports.randomString = randomString;

function getRandomArrayElement(myArray) {
    var compactedArray = ah.compactArray(myArray);
    return compactedArray[randomNum(compactedArray.length)];
}
module.exports.getRandomArrayElement = getRandomArrayElement;