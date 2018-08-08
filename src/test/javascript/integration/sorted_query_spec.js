require('../integration_config');
const {
    createChannel,
    fromObjectPath,
    getProp,
    hubClientGet,
} = require('../lib/helpers');
const moment = require('moment');
const channelName = utils.randomChannelName();
const channelResource = `${channelUrl}/${channelName}`;
let createdChannel = false;
const headers = { 'Content-Type': 'application/json' };

/**
 * This should:
 *
 * 1 - create a channel
 * 2 - post items into the channel
 * 3 - query channel ensuring specific specific order
 *
 */

function trimWordRandomly (word) {
    if (!word) return '';
    const trimLocation = Math.floor(Math.random() * (word.length - 2)) + 2;
    return word.slice(0, trimLocation);
}

describe(__filename, () => {
    function expectURIsInAscendingOrder (response) {
        expect(getProp('statusCode', response)).toBe(200);
        const uris = fromObjectPath(['body', '_links', 'uris'], response);
        console.log('uris:', uris);
        expect(uris).toEqual(postedItems);
    }

    function expectURIsInDescendingOrder (response) {
        expect(getProp('statusCode', response)).toBe(200);
        const uris = fromObjectPath(['body', '_links', 'uris'], response);
        console.log('uris:', uris);
        let reversedItems = postedItems.slice().reverse();
        expect(uris).toEqual(reversedItems);
    }

    let postedItems = [];
    beforeAll(async () => {
        const channel = await createChannel(channelName);
        if (getProp('statusCode', channel) === 201) {
            createdChannel = true;
            console.log(`created channel for ${__filename}`);
        }
    });

    it('posts four items', (done) => {
        if (!createdChannel) return done.fail('channel not created in before block');
        let headers = {'Content-Type': 'plain/text'};
        utils.httpPost(channelResource, headers, moment.utc().toISOString())
            .then(response => {
                expect(getProp('statusCode', response)).toEqual(201);
                const selfLink = fromObjectPath(['body', '_links', 'self', 'href'], response);
                postedItems.push(selfLink);
                return utils.httpPost(channelResource, headers, moment.utc().toISOString());
            })
            .then(response => {
                expect(getProp('statusCode', response)).toEqual(201);
                const selfLink = fromObjectPath(['body', '_links', 'self', 'href'], response);
                postedItems.push(selfLink);
                return utils.httpPost(channelResource, headers, moment.utc().toISOString());
            })
            .then(response => {
                expect(getProp('statusCode', response)).toEqual(201);
                const selfLink = fromObjectPath(['body', '_links', 'self', 'href'], response);
                postedItems.push(selfLink);
                return utils.httpPost(channelResource, headers, moment.utc().toISOString());
            })
            .then(response => {
                expect(getProp('statusCode', response)).toEqual(201);
                const selfLink = fromObjectPath(['body', '_links', 'self', 'href'], response);
                postedItems.push(selfLink);
            })
            .finally(() => {
                console.log('postedItems:', postedItems);
                expect(postedItems.constructor).toBe(Array);
                expect(postedItems.length).toEqual(4);
                done();
            });
    });

    it('gets latest ascending items', async () => {
        if (!createdChannel) return fail('channel not created in before block');
        let order = trimWordRandomly('ascending');
        const response = await hubClientGet(`${channelResource}/latest/4?stable=false&order=${order}`, headers);
        expectURIsInAscendingOrder(response);
    });

    it('gets latest baloney order items', async () => {
        if (!createdChannel) return fail('channel not created in before block');
        let order = 'baloney';
        const response = await hubClientGet(`${channelResource}/latest/4?stable=false&order=${order}`, headers);
        expectURIsInAscendingOrder(response);
    });

    // secondDo(`${channelResource}/earliest/4?stable=false&order=desc`, descendingItems, done);
    it('gets descending earliest', async () => {
        if (!createdChannel) return fail('channel not created in before block');
        let order = trimWordRandomly('descending');
        const response = await hubClientGet(`${channelResource}/earliest/4?stable=false&order=${order}`, headers);
        expectURIsInDescendingOrder(response);
    });

    it('gets descending latest', async () => {
        if (!createdChannel) return fail('channel not created in before block');
        let order = trimWordRandomly('descending');
        const response = await hubClientGet(`${channelResource}/latest/4?stable=false&order=${order}`, headers);
        expectURIsInDescendingOrder(response);
    });

    it('gets descending next', async () => {
        if (!createdChannel) return fail('channel not created in before block');
        let oneMinuteAgo = moment.utc().subtract(1, 'minute').format('YYYY/MM/DD/HH/mm/ss/SSS');
        let order = trimWordRandomly('descending');
        const response = await hubClientGet(`${channelResource}/${oneMinuteAgo}/A/next/4?stable=false&order=${order}`, headers);
        expectURIsInDescendingOrder(response);
    });

    it('gets descending previous', async () => {
        if (!createdChannel) return fail('channel not created in before block');
        let oneMinuteInTheFuture = moment.utc().add(1, 'minute').format('YYYY/MM/DD/HH/mm/ss/SSS');
        let order = trimWordRandomly('descending');
        const response = await hubClientGet(`${channelResource}/${oneMinuteInTheFuture}/A/previous/4?stable=false&order=${order}`, headers);
        expectURIsInDescendingOrder(response);
    });

    it('gets descending hour', async () => {
        if (!createdChannel) return fail('channel not created in before block');
        let now = moment.utc().format('YYYY/MM/DD/HH');
        let order = trimWordRandomly('descending');
        const response = await hubClientGet(`${channelResource}/${now}?stable=false&order=${order}`, headers);
        expectURIsInDescendingOrder(response);
    });

    it('bulk get', async () => {
        if (!createdChannel) return fail('channel not created in before block');
        const oneMinuteAgo = moment.utc().subtract(1, 'minute').format('YYYY/MM/DD/HH/mm/ss/SSS');
        const order = trimWordRandomly('descending');
        const url = `${channelResource}/${oneMinuteAgo}/A/next/4?stable=false&order=${order}&bulk=true`;
        const response = await hubClientGet(url);
        expect(getProp('statusCode', response)).toBe(200);
        const body = getProp('body', response) || [];
        // console.log(body);
        const descendingItems = postedItems.slice().reverse();
        const first = body.indexOf(descendingItems[0]);
        const second = body.indexOf(descendingItems[1]);
        const third = body.indexOf(descendingItems[2]);
        const fourth = body.indexOf(descendingItems[3]);

        // all the items should be present
        expect(first).not.toEqual(-1);
        expect(second).not.toEqual(-1);
        expect(third).not.toEqual(-1);
        expect(fourth).not.toEqual(-1);

        // they should be in the correct order
        expect(first).toBeLessThan(second);
        expect(second).toBeLessThan(third);
        expect(third).toBeLessThan(fourth);
    });
});
