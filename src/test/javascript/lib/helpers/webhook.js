const rp = require('request-promise-native');
const { getProp } = require('./functional');

const getWebhookUrl = () => {
    if (Math.random() > 0.5) {
        return `${hubUrlBase}/webhook`;
    }
    return `${hubUrlBase}/group`;
};

const putWebhook = async (groupName, groupConfig, status, description, groupUrl) => {
    const formattedDescription = description || 'none';
    const expectedStatus = status || 201;
    const webhookUrl = groupUrl || getWebhookUrl();
    const groupResource = `${webhookUrl}/${groupName}`;
    console.log(`creating group ${groupName} for ${formattedDescription}`);
    try {
        const response = await rp({
            method: 'PUT',
            url: groupResource,
            headers: { "Content-Type": "application/json" },
            body: groupConfig,
            resolveWithFullResponse: true,
            json: true,
        });
        const statusCode = getProp('statusCode', response);
        console.log('expected status', expectedStatus);
        console.log('actual status', statusCode);
        return response || {};
    } catch (ex) {
        console.log('error creating webhook ', ex.message);
        return ex || {};
    }
};

module.exports = {
    getWebhookUrl,
    putWebhook,
};
