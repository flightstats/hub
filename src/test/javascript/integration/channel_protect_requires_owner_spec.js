require('../integration_config');
const { getProp, hubClientPut } = require('../lib/helpers');
const {
    getChannelUrl,
} = require('../lib/config');

const channelUrl = getChannelUrl();
const headers = { 'Content-Type': 'application/json' };
describe(__filename, function () {
    /**
     * When creating a new channel and protect is true,
     * the owner field must also be provided.
     */

    describe('new channel', function () {
        const channelResource = `${channelUrl}/${utils.randomChannelName()}`;
        it('returns 400 on create protected channel without owner set', async () => {
            const response = await hubClientPut(channelResource, headers, { protect: true });
            expect(getProp('statusCode', response)).toEqual(400);
        });
        it('returns 201 on create protected channel with owner set', async () => {
            const response = await hubClientPut(channelResource, headers, { protect: true, owner: 'someone' });
            expect(getProp('statusCode', response)).toEqual(201);
        });
    });

    /**
     * When updating an existing channel to be protected,
     * the owner field must already exist if not provided.
     */

    describe('update channel with existing owner', function () {
        const channelResource = `${channelUrl}/${utils.randomChannelName()}`;
        it('creates default channel no owner set', async () => {
            const response = await hubClientPut(channelResource, headers, {});
            expect(getProp('statusCode', response)).toEqual(201);
        });
        it('returns 400 on attempt to update default channel to protect with no owner', async () => {
            const response = await hubClientPut(channelResource, headers, { protect: true });
            expect(getProp('statusCode', response)).toEqual(400);
        });
        it('successfully updates channel with an owner', async () => {
            const response = await hubClientPut(channelResource, headers, { owner: 'someone' });
            expect(getProp('statusCode', response)).toEqual(201);
        });
        it('updates channel to protect now that owner is set', async () => {
            const response = await hubClientPut(channelResource, headers, { protect: true });
            expect(getProp('statusCode', response)).toEqual(201);
        });
    });

    /**
     * When updating an existing channel to be protected,
     * the owner field must be provided if it doesn't already exist.
     */

    describe('update channel without existing owner', function () {
        const channelResource = `${channelUrl}/${utils.randomChannelName()}`;
        it('creates default channel no owner set', async () => {
            const response = await hubClientPut(channelResource, headers, {});
            expect(getProp('statusCode', response)).toEqual(201);
        });
        it('returns 400 on attempt to update default channel to protect with no owner', async () => {
            const response = await hubClientPut(channelResource, headers, { protect: true });
            expect(getProp('statusCode', response)).toEqual(400);
        });
        it('successfully updates channel with an owner and protect true', async () => {
            const response = await hubClientPut(channelResource, headers, { owner: 'someone' });
            expect(getProp('statusCode', response)).toEqual(201);
        });
    });
});
