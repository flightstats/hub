require('./integration_config.js');

var request = require('request');
var http = require('http');
var parse = require('parse-link-header');
var channel = utils.randomChannelName();
var moment = require('moment');

var tag = Math.random().toString().replace(".", "");
var testName = __filename;


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
describe(testName, function () {

    var mutableTime = moment.utc().subtract(3, 'years');

    var channelBody = {
        mutableTime: mutableTime.format('YYYY-MM-DDTHH:mm:ss.SSS'),
        tags: [tag, "test"]
    };

    utils.putChannel(channel, false, channelBody, testName);

    var channelURL = hubUrlBase + '/channel/' + channel;

    var items = [];

    function getFormattedUrl(time) {
        return channelURL + time.format('/YYYY/MM/DD/HH/mm/ss/SSS');
    }

    var earliestTime = mutableTime.subtract(2, 'years');

    it('posts historical items to ' + channel, function (done) {
        utils.postItemQ(getFormattedUrl(earliestTime))
            .then(function (value) {
                items.push(value.response.headers.location)
                return utils.postItemQ(getFormattedUrl(earliestTime.add(1, 'years')));
            })
            .then(function (value) {
                items.push(value.response.headers.location)
                return utils.postItemQ(getFormattedUrl(earliestTime.add(6, 'months')));
            })
            .then(function (value) {
                items.push(value.response.headers.location)
                done();
            })
        ;
    });

    it('posts live items to ' + channel, function (done) {
        utils.postItemQ(channelURL)
            .then(function (value) {
                items.push(value.response.headers.location)
                return utils.postItemQ(channelURL);
            })
            .then(function (value) {
                items.push(value.response.headers.location)
                return utils.postItemQ(channelURL);
            })
            .then(function (value) {
                items.push(value.response.headers.location)
                console.log('items', items);
                done();
            })
        ;
    });

    var parameters = "?trace=true&stable=false";
    var next7 = earliestTime.subtract(1, 'month').format('/YYYY/MM/DD/HH/mm/ss/SSS') + "/0/next/7" + parameters;

    it('queries next 7 All ' + next7, function (done) {
        utils.getQuery(channelURL + next7 + '&epoch=ALL', 200, items, done);
    });

    it('queries next 7 Immutable ' + next7, function (done) {
        utils.getQuery(channelURL + next7 + '&epoch=IMMUTABLE', 200, items.slice(3), done);
    });

    it('queries next 7 Mutable ' + next7, function (done) {
        utils.getQuery(channelURL + next7 + '&epoch=MUTABLE', 200, items.slice(0, 3), done);
    });

    var channelBodyChange = {
        mutableTime: moment(earliestTime).add(1, 'years').format('YYYY-MM-DDTHH:mm:ss.SSS'),
        tags: [tag, "test"]
    };

    utils.putChannel(channel, false, channelBodyChange, testName);

    it('queries next 7 Immutable after change ' + next7, function (done) {
        utils.getQuery(channelURL + next7 + '&epoch=IMMUTABLE', 200, items.slice(2), done);
    }, 3 * 60 * 1000);

    it('queries next 7 Mutable after change' + next7, function (done) {
        utils.getQuery(channelURL + next7 + '&epoch=MUTABLE', 200, items.slice(0, 2), done);
    }, 3 * 60 * 1000);
    it('queries earliest 2 Immutable after change ', function (done) {
        utils.getQuery(channelURL + "/earliest/2" + parameters + '&epoch=IMMUTABLE', 200, items.slice(2, 4), done);
    }, 5 * 60 * 1000);

    it('queries earliest 2 Mutable after change ', function (done) {
        utils.getQuery(channelURL + "/earliest/2" + parameters + '&epoch=MUTABLE', 200, items.slice(0, 2), done);
    }, 5 * 60 * 1000);
});
