const EventSource = require('eventsource');
const {
    createChannel,
    fromObjectPath,
    getProp,
    hubClientDelete,
    hubClientPostTestItem,
    itSleeps,
    randomChannelName,
    waitForCondition,
} = require('../lib/helpers');
const {
    getChannelUrl,
} = require('../lib/config');

const channelUrl = getChannelUrl();
const channelName = randomChannelName();
const channelResource = `${channelUrl}/${channelName}`;

/**
 * This should:
 *
 * 1 - create a channel
 * 2 - start the events events on that channel
 * 3 - post items to the channel
 * 4 - verify that the events are returned within delta time
 */
describe(__filename,  () => {
    const events = [];
    const postedItems = [];
    let createdChannel = false;

    beforeAll(async () => {
        const channel = await createChannel(channelName, false, __filename);
        if (getProp('statusCode', channel) === 201) {
            console.log(`created channel for ${__filename}`);
            createdChannel = true;
        }
    });

    beforeEach( async () => {
        if (!createdChannel) return fail('channel not created in before block');
        const source = new EventSource(
            `${channelResource}/events`,
            { headers: { 'Accept-Encoding': 'gzip' } }
        );
        source.addEventListener('application/json',  (event) =>  {
            console.log('message', event);
            events.push(getProp('lastEventId', event));
        });

        source.addEventListener('open',  (event) => {
            console.log(`opened ${event}`);
        });
        console.log("!#$%!@#$%@#$%@#$%@#$%@#$%@#$%");
        await itSleeps(1000);
    });

    it('verifies events in sequence posted to channel', async () => {
        // given
        if (!createdChannel) return fail('channel not created in before block');
        const response1 = await hubClientPostTestItem(channelResource);
        const response2 = await hubClientPostTestItem(channelResource);
        const response3 = await hubClientPostTestItem(channelResource);
        const response4 = await hubClientPostTestItem(channelResource);
        const response5 = await hubClientPostTestItem(channelResource);
        const posted = [response1, response2, response3, response4, response5]
            .map(value => {
                return fromObjectPath(['body', '_links', 'self', 'href'], value);
            });
        expect(posted.length).toEqual(5);
        postedItems.push(...posted);

        // when
        await waitForCondition(() => postedItems.length === events.length);

        // then
        console.log('events:', events);
        expect(postedItems.length).toBeGreaterThan(0);
        const actual = postedItems.every((item, index) => {
            return item === events[index];
        });
        expect(actual).toBe(true);
    });

    it('verifies events regardless of sequence posted to channel', async () => {
        // given
        if (!createdChannel) return fail('channel not created in before block');
        const postItem = () => hubClientPostTestItem(channelResource);
        const response = await Promise.all( Array(5).fill(postItem)
            .map(async func => func()));
        const posted = response.map(value => {
            return fromObjectPath(['body', '_links', 'self', 'href'], value);
        });
        expect(posted.length).toBeGreaterThanOrEqual(5);
        postedItems.push(...posted);

        // when
        await waitForCondition(() => postedItems.length === events.length);

        // then
        console.log('events:', events);
        expect(postedItems.length).toBeGreaterThan(0);
        const actual = postedItems.every((item) => {
            return !!events.includes(item);
        });
        expect(actual).toBe(true);
    });

    afterAll(async () => {
        await hubClientDelete(channelResource);
    });
});
