require('../integration_config');
const {
    fromObjectPath,
    getProp,
    hubClientChannelRefresh,
    hubClientPut,
    hubClientPostTestItem,
} = require('../lib/helpers');

const channel = utils.randomChannelName();
const moment = require('moment');
const channelResource = `${channelUrl}/${channel}`;
const headers = { 'Content-Type': 'application/json' };
const mutableTime = moment.utc().subtract(3, 'years');
const tag = Math.random().toString().replace(".", "");
const channelBody = {
    mutableTime: mutableTime.format('YYYY-MM-DDTHH:mm:ss.SSS'),
    tags: [tag, "test"],
};
const earliestTime = mutableTime.subtract(2, 'years');
const channelBodyChange = {
    mutableTime: moment(earliestTime).add(1, 'years').format('YYYY-MM-DDTHH:mm:ss.SSS'),
    tags: [tag, "test"],
};
const parameters = "?trace=true&stable=false";
const next7 = earliestTime.subtract(1, 'month').format('/YYYY/MM/DD/HH/mm/ss/SSS') + "/0/next/7" + parameters;
const items = [];
const getFormattedUrl = time =>
    `${channelResource}${time.format('/YYYY/MM/DD/HH/mm/ss/SSS')}`;
/**
 * This should:
 * Create a channel with mutableTime
 * add 3 historical items
 * add 3 live items
 *
 * Query items by direction, verify exclusion
 *
 * Change mutableTime to include one historical item
 * Query items by direction, verify exclusion
 */
describe(__filename, function () {
    beforeAll(async () => {
        const response = await hubClientPut(channelResource, headers, channelBody);
        expect(getProp('statusCode', response)).toEqual(201);
    });

    it(`posts historical items to  ${channel}`, async () => {
        const response = await hubClientPostTestItem(getFormattedUrl(earliestTime));
        items.push(fromObjectPath(['headers', 'location'], response));
        const response1 = await hubClientPostTestItem(getFormattedUrl(earliestTime.add(1, 'years')));
        items.push(fromObjectPath(['headers', 'location'], response1));
        const response2 = await hubClientPostTestItem(getFormattedUrl(earliestTime.add(6, 'months')));
        items.push(fromObjectPath(['headers', 'location'], response2));
    });

    it(`posts live items to ${channel}`, async () => {
        const response = await hubClientPostTestItem(channelResource);
        items.push(fromObjectPath(['headers', 'location'], response));
        const response1 = await hubClientPostTestItem(channelResource);
        items.push(fromObjectPath(['headers', 'location'], response1));
        const response2 = await hubClientPostTestItem(channelResource);
        items.push(fromObjectPath(['headers', 'location'], response2));
    });

    it(`queries next 7 All ${next7}`, function (done) {
        utils.getQuery(`${channelResource}${next7}&epoch=ALL`, 200, items, done);
    });

    it(`queries next 7 Immutable ${next7}`, function (done) {
        utils.getQuery(`${channelResource}${next7}&epoch=IMMUTABLE`, 200, items.slice(3), done);
    });

    it(`queries next 7 Mutable ${next7}`, function (done) {
        utils.getQuery(`${channelResource}${next7}&epoch=MUTABLE`, 200, items.slice(0, 3), done);
    });

    it('updates the mutableTime value', async () => {
        const response = await hubClientPut(channelResource, headers, channelBodyChange);
        expect(getProp('statusCode', response)).toEqual(201);
    });

    it('waits while the channel is refreshed', async () => {
        const response = await hubClientChannelRefresh();
        expect(getProp('statusCode', response)).toEqual(200);
    });

    it(`queries next 7 Immutable after change ${next7}`, function (done) {
        utils.getQuery(`${channelResource}${next7}&epoch=IMMUTABLE`, 200, items.slice(2), done);
    }, 3 * 60 * 1000);

    it(`queries next 7 Mutable after change${next7}`, function (done) {
        utils.getQuery(`${channelResource}${next7}&epoch=MUTABLE`, 200, items.slice(0, 2), done);
    }, 3 * 60 * 1000);
    it('queries earliest 2 Immutable after change ', function (done) {
        utils.getQuery(`${channelResource}/earliest/2${parameters}&epoch=IMMUTABLE`, 200, items.slice(2, 4), done);
    }, 5 * 60 * 1000);

    it('queries earliest 2 Mutable after change ', function (done) {
        utils.getQuery(`${channelResource}/earliest/2${parameters}&epoch=MUTABLE`, 200, items.slice(0, 2), done);
    }, 5 * 60 * 1000);
});
