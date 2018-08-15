require('../integration_config');
const EventSource = require('eventsource');
const {
    createChannel,
    fromObjectPath,
    getProp,
    hubClientPostTestItem,
} = require('../lib/helpers');

const channelName = utils.randomChannelName();
const channelResource = `${channelUrl}/${channelName}`;

/**
 * This should:
 *
 * 1 - create a channel
 * 2 - start the events events on that channel
 * 3 - post items to the channel
 * 4 - verify that the events are returned within delta time
 */
describe(__filename, function () {
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

    it('creates event source', function () {
        if (!createdChannel) return fail('channel not created in before block');
        const source = new EventSource(
            `${channelResource}/events`,
            { headers: { 'Accept-Encoding': 'gzip' } }
        );

        source.addEventListener('application/json', function (e) {
            console.log('message', e);
            events.push(getProp('lastEventId', e));
        }, false);

        source.addEventListener('open', function (e) {
            console.log('opened');
        }, false);
    });

    utils.itSleeps(1000);

    it('posts items sequentially', async () => {
        if (!createdChannel) return fail('channel not created in before block');
        const response1 = await hubClientPostTestItem(channelResource);
        const response2 = await hubClientPostTestItem(channelResource);
        const response3 = await hubClientPostTestItem(channelResource);
        const response4 = await hubClientPostTestItem(channelResource);
        const response5 = await hubClientPostTestItem(channelResource);
        const actual = [response1, response2, response3, response4, response5]
            .map(value => {
                return fromObjectPath(['body', '_links', 'self', 'href'], value);
            });
        expect(actual.length).toEqual(5);
        postedItems.push(...actual);
    }, 10 * 1000);

    it('waits for data', function (done) {
        if (!createdChannel) return done.fail('channel not created in before block');
        utils.waitForData(postedItems, events, done);
    });

    it('verifies events in sequence posted to channel', () => {
        console.log('events:', events);
        expect(postedItems.length).toBeGreaterThan(0);
        const actual = postedItems.every((item, index) => {
            return item === events[index];
        });
        expect(actual).toBe(true);
    });

    it('posts items in no particular order', async () => {
        if (!createdChannel) return fail('channel not created in before block');
        const postItem = () => hubClientPostTestItem(channelResource);
        const responses = [1, 2, 3, 4, 5]
            .map(i => postItem());
        const response = await Promise.all(responses);
        const actual = response.map(value => {
            return fromObjectPath(['body', '_links', 'self', 'href'], value);
        });
        expect(actual.length).toEqual(5);
        postedItems.push(...actual);
    }, 10 * 1000);

    it('waits for data', function (done) {
        if (!createdChannel) return done.fail('channel not created in before block');
        utils.waitForData(postedItems, events, done);
    });

    it('verifies events regardless of sequence posted to channel', () => {
        console.log('events:', events);
        expect(postedItems.length).toBeGreaterThan(0);
        const actual = postedItems.every((item) => {
            return !!events.includes(item);
        });
        expect(actual).toBe(true);
    });
});
