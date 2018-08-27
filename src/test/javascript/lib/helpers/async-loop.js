const toChunkArray = (arr, limit) => {
    const newArray = [];
    let amountToSplice = limit;
    do {
        amountToSplice = arr.length >= limit ? limit : arr.length;
        console.log('amountToSplice', amountToSplice);
        newArray.push(arr.splice(0, amountToSplice));
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
