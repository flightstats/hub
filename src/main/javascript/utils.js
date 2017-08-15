exports.getTimestamp = () => {
    return new Date().toISOString();
};

exports.logParameters = (parameters) => {
    let line = new Array(50).join('-');
    console.log(line);
    Object.entries(parameters).forEach(([key, value]) => {
        console.log(`${key}: ${value}`);
    });
    console.log(line);
};
