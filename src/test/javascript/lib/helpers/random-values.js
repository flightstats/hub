const DEFAULT_LIMIT = 60 * 1000;

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

const randomItemsFromArrayByPercentage = (arr, percentage, limit = DEFAULT_LIMIT) => {
    const resultArray = [];
    const amountToTake = Math.floor((arr.length * percentage) / 100);
    const finalAmount = amountToTake < limit ? amountToTake : limit;
    console.log(`taking ${percentage}% of the ${arr.length} items: `, amountToTake);
    if (finalAmount === limit) console.log(`OVERRIDING percentage with limit: ${limit}`);
    do {
        resultArray.push(arr[Math.floor(Math.random() * arr.length)]);
    } while (resultArray.length < finalAmount);
    return resultArray;
};

module.exports = {
    randomChannelName,
    randomItemsFromArrayByPercentage,
    randomNumberBetweenInclusive,
    randomString,
    randomTag,
};
