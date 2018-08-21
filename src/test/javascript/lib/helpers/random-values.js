
const randomChannelName = () => `TeSt_${Math.random().toString().replace(".", "_")}`;

const alpha = 'abcdefghijklmnopqrstuvwxyz';

const randomNumberBetweenInclusive = (min = 0, max) => {
    const minCalc = Math.ceil(min);
    const maxCalc = Math.floor(max);
    return Math.floor(Math.random() * (maxCalc - minCalc + 1)) + minCalc;
};

const randomString = (len) => {
    let str = '';
    for (let i = 0; i <= len; i++) {
        const charIndex = randomNumberBetweenInclusive(i, 26);
        const char = alpha.charAt(charIndex);
        str = `${str}${char}`;
    }
    return str;
};

const randomTag = () => `tag${Math.random().toString().replace(".", "")}`;

module.exports = {
    randomChannelName,
    randomNumberBetweenInclusive,
    randomString,
    randomTag,
};
