// restructure an array into an array of arrays
// each array in the array will be of LIMIT or less size
const toChunkArray = (arr, limit) => {
    const newArray = [];
    // create a copy to avoid mutating the original
    const arrayCopy = [];
    arrayCopy.push(...arr);
    let amountToSplice = limit;
    do {
        amountToSplice = arrayCopy.length >= limit ? limit : arrayCopy.length;
        console.log('amountToSplice', amountToSplice);
        newArray.push(arrayCopy.splice(0, amountToSplice));
    } while (amountToSplice > 0);
    return newArray;
};

const processChunks = async (chunks, asyncCallback) => {
    const totalCodes = [];
    for (const chunk of chunks) {
        const statusCodes = await Promise.all(chunk.map(uri => asyncCallback(uri)));
        totalCodes.push(...statusCodes);
    };
    return totalCodes;
};

module.exports = {
    processChunks,
    toChunkArray,
};
