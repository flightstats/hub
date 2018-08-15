require('../integration_config');
const { getProp, fromObjectPath, hubClientPut, hubClientPostTestItem } = require('../lib/helpers');
const channel = utils.randomChannelName();
const channelResource = `${channelUrl}/${channel}`;
const headers = { 'Content-Type': 'application/json' };
const moment = require('moment');
const mutableTime = moment.utc().subtract(1, 'minute');
const items = [];
const channelBody = {
    mutableTime: mutableTime.format('YYYY-MM-DDTHH:mm:ss.SSS'),
    tags: ["test"],
};
let latest = null;
/**
 * create a channel
 * post 2 items
 * gets the item back out with latest
 * get both items back out with latest/10
 */
describe(__filename, function () {
    beforeAll(async () => {
        const response = await hubClientPut(channelResource, headers, channelBody);
        expect(getProp('statusCode', response)).toEqual(201);
        const historicalItem1 = `${channelResource}${moment(mutableTime).subtract(2, 'minute').format('/YYYY/MM/DD/HH/mm/ss/SSS')}`;
        const historicalItem2 = `${channelResource}${mutableTime.format('/YYYY/MM/DD/HH/mm/ss/SSS')}`;
        const response1 = await hubClientPostTestItem(historicalItem1);
        const response2 = await hubClientPostTestItem(historicalItem2);
        const actual = [response1, response2]
            .map(res => fromObjectPath(['headers', 'location'], res));
        items.push(...actual);
    });

    it("gets latest in default Epoch in channel ", function (done) {
        utils.getLocation(channelResource + '/latest?trace=true', 404, false, done);
    });

    it("gets latest Immutable in channel ", function (done) {
        utils.getLocation(channelResource + '/latest?epoch=IMMUTABLE', 404, false, done);
    });

    it("gets latest Mutable in channel ", function (done) {
        utils.getLocation(channelResource + '/latest?epoch=MUTABLE', 303, items[1], done);
    });

    it('posts item now', async () => {
        const response = await hubClientPostTestItem(channelResource);
        const latest = fromObjectPath(['headers', 'location'], response);
        items.push(latest);
    });

    it("gets latest in Immutable in channel - after now item", function (done) {
        utils.getLocation(channelResource + '/latest?stable=false', 303, latest, done);
    });

    it("gets latest Mutable in channel - after now item ", function (done) {
        utils.getLocation(channelResource + '/latest?epoch=MUTABLE&trace=true', 303, items[1], done);
    });

    it("gets latest N Mutable in channel ", function (done) {
        utils.getQuery(channelResource + '/latest/10?epoch=MUTABLE', 200, items.slice(0, 2), done);
    });

    it("gets latest N ALL in channel ", function (done) {
        utils.getQuery(channelResource + '/latest/10?stable=false&epoch=ALL', 200, items, done);
    });
});
