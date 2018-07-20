const axios = require('axios');
// const { getProp } = require('./functional');

module.exports.getHubItem = async (uri) => {
    try {
        const result = await axios.get(uri, { responseEncoding: null });
        return result || {};
    } catch (ex) {
        console.log('got error ', uri, ex);
        return {};
    }
};
