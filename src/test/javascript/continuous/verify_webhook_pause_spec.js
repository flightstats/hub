// This test is intended to be run as a jenkins scheduled "continuous" test on pdx dev
const { getChannelUrl } = require('../lib/config');
const {
    getProp,
    hubClientGet,
    hubClientPut,
} = require('../lib/helpers');
const channelName = "destination";
const channelResource = `${getChannelUrl()}/${channelName}`;
const webhookurl = 'http://hub.iad.dev.flightstats.io/webhook/Repl_hub_ucs_dev_destination';
const webhookName = "Repl_hub_ucs_dev_destination";
let pause = null;
let webhookConfig = null;
const headers = { "Content-Type": "application/json" };
// TODO: this is not being used
describe(__filename, function () {
    it(`gets channel ${channelName}`, async () => {
        console.log('get channel ', channelResource, ' for ', __filename);
        const response = await hubClientGet(channelResource, headers);
        expect(getProp('statusCode', response)).toBe(200);
    });

    it('verify webhook ', async () => {
        const response = await hubClientGet(webhookurl, headers);
        expect(getProp('statusCode', response)).toBe(200);
        const body = getProp('body', response);
        webhookConfig = body;
        pause = webhookConfig.paused;
        console.log("is it paused? ", pause);
        webhookConfig.paused = !pause;
    });

    it(`creates group ${webhookName}`, async () => {
        console.log('creating group ', webhookName, ' for ', __filename);
        const response = await hubClientPut(webhookurl, headers, JSON.stringify(webhookConfig));
        const body = getProp('body', response);
        console.log('webhook body response ', getProp('body', response));
        expect(body.paused).toBe(!pause);
    });
});
