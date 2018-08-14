require('../integration_config');
const { getProp, fromObjectPath, hubClientPut } = require('../lib/helpers');

const channel = utils.randomChannelName();
const channelResource = `${channelUrl}/${channel}`;
const headers = { 'Content-Type': 'application/json' };
const moment = require('moment');
const mutableTime = moment.utc().subtract(1, 'minute');
const channelBody = {
    mutableTime: mutableTime.format('YYYY-MM-DDTHH:mm:ss.SSS'),
    tags: ["test"],
};
const items = [];
/**
 * create a channel
 * post 2 historical items
 * gets the item back out with earliest
 * post a current item
 * get items back out with earliest/10
 */
describe(__filename, function () {
    beforeAll(async () => {
        const response = await hubClientPut(channelResource, headers, channelBody);
        expect(getProp('statusCode', response)).toEqual(201);
    });

    it('posts two historical items', function (done) {
        const historicalItem1 = `${channelResource}/2013/11/20/12/00/00/000`;
        const historicalItem2 = `${channelResource}/2013/11/20/12/01/00/000`;
        utils.postItemQ(historicalItem1)
            .then(function (value) {
                const location = fromObjectPath(['response', 'headers', 'location'], value);
                items.push(location);
                return utils.postItemQ(historicalItem2);
            })
            .then(function (nextValue) {
                const nextLocation = fromObjectPath(['response', 'headers', 'location'], nextValue);
                items.push(nextLocation);
                done();
            });
    });

    it("gets earliest in default Epoch in channel ", function (done) {
        utils.getLocation(`${channelResource}/earliest?trace=true`, 404, false, done);
    });

    it("gets earliest Immutable in channel missing ", function (done) {
        utils.getLocation(`${channelResource}/earliest?epoch=IMMUTABLE`, 404, false, done);
    });

    it("gets earliest Mutable in channel", function (done) {
        utils.getLocation(`${channelResource}/earliest?epoch=MUTABLE`, 303, items[0], done);
    });

    it('posts item now', function (done) {
        utils.postItemQ(channelResource)
            .then(function (value) {
                const location = fromObjectPath(['response', 'headers', 'location'], value);
                items.push(location);
                done();
            });
    });

    it("gets earliest Immutable in channel - after now item", function (done) {
        utils.getLocation(`${channelResource}/earliest?stable=false&trace=true`, 303, items[2], done);
    });

    it("gets earliest Mutable in channel - after now item", function (done) {
        utils.getLocation(`${channelResource}/earliest?epoch=MUTABLE`, 303, items[0], done);
    });

    it("gets earliest N Mutable in channel ", function (done) {
        utils.getQuery(`${channelResource}/earliest/10?epoch=MUTABLE`, 200, items.slice(0, 2), done);
    });

    it("gets earliest N ALL in channel ", function (done) {
        utils.getQuery(`${channelResource}/earliest/10?stable=false&epoch=ALL`, 200, items, done);
    });
});
