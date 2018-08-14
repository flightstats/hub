const getWebhookUrl = () => {
    if (Math.random() > 0.5) {
        return `${hubUrlBase}/webhook`;
    }
    return `${hubUrlBase}/group`;
};

module.exports = {
    getWebhookUrl,
};
