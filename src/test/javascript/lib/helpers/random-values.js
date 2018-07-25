
module.exports.randomChannelName = () => `TeSt_${Math.random().toString().replace(".", "_")}`;

module.exports.randomNumberBetweenInclusive = (min, max) => {
    const minCalc = Math.ceil(min);
    const maxCalc = Math.floor(max);
    return Math.floor(Math.random() * (maxCalc - minCalc + 1)) + minCalc;
};

module.exports.randomTag = () => `tag${Math.random().toString().replace(".", "")}`;
