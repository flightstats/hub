const os = require('os');
const { getProp } = require('./functional');

const getHubDomain = () => {
    const interfaces = os.networkInterfaces() || [];
    const isIp = (arr) => arr &&
        arr.find(obj =>
            getProp('family', obj) === 'IPv4' &&
            !getProp('address', obj).includes('127')
        );
    return process.env.IP || Object.keys(interfaces)
        .reduce((a, b) => {
            const ipObject = isIp(interfaces[b]);
            return getProp('address', ipObject) || a;
        }, '');
};
module.exports = {
    getHubDomain,
};
