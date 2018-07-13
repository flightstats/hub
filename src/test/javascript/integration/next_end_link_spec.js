require('../integration_config');
const {
    fromObjectPath,
    getProp,
} = require('../lib/helpers');

var channelName = utils.randomChannelName();
var channelResource = channelUrl + "/" + channelName;
var testName = __filename;

describe(testName, function () {
    utils.createChannel(channelName);

    console.log('channelName', channelName);

    it('adds item and checks relative links', function (done) {
        var itemHref;
        utils.postItemQ(channelResource)
            .then(function (value) {
                itemHref = fromObjectPath(['body', '_links', 'self', 'href'], value);
                console.log('item_link', itemHref);
                return utils.getQ(itemHref + '/next/10');
            })
            .then(function (value) {
                const links = fromObjectPath(['body', '_links'], value) || {};
                const { previous = {}, uris } = links;
                const urisLength = uris && uris.length === 0;
                expect(urisLength).toBe(true);
                expect(previous.href).toBeDefined();
                expect(previous.href).toBe(itemHref + '/previous/10?stable=false');
                done();
            });
    });

});
