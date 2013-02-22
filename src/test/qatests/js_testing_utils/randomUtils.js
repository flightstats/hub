/**
 * Created with JetBrains WebStorm.
 * User: gnewcomb
 * Date: 2/14/13
 * Time: 2:13 PM
 * To change this template use File | Settings | File Templates.
 */

// returns 0 through (hi - 1)
function randomNum(hi){
    return Math.floor(Math.random()*hi);
}
function randomChar(){
    return String.fromCharCode(randomNum(100));
}

function limitedRandomChar() {
    //var allowedChars = 'abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789.-_';
    var allowedChars = 'abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789';
    return allowedChars[randomNum(allowedChars.length)];
}

function simulatedTextChar(res) {
    var res = randomNum(100) + 1;
    var l, poss = '';

    // last minute hack for paragraphs, should rewrite:
    var paragraphRes = randomNum(200) + 1;
    if (200 == paragraphRes) {
        return "\r\n";
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

function randomString(length, fChar){
    if (length == 0) {
        length = 1;
    }

    var myCharFunc = (arguments.length > 1 ? fChar : randomChar);
    var str = "";
    for(var i = 0; i < length; i = i + 1){
        str += myCharFunc();
    }
    return str;
}




module.exports.randomNum = randomNum;
module.exports.randomChar = randomChar;
module.exports.randomString = randomString;
module.exports.limitedRandomChar = limitedRandomChar;
module.exports.simulatedTextChar = simulatedTextChar;