require('../integration_config');

var request = require('request');
var http = require('http');
var moment = require('moment');
var channelName = utils.randomChannelName();
var channelResource = `${channelUrl}/${channelName}`;

/**
 * This should:
 *
 * 1 - create a channel
 * 2 - post items into the channel
 * 3 - query channel ensuring specific specific order
 *
 */

describe(__filename, () => {

    let postedItems = [];

    utils.createChannel(channelName);

    it('posts four items', (done) => {
        let headers = {'Content-Type': 'plain/text'};
        utils.httpPost(channelResource, headers, moment.utc().toISOString())
            .then(response => {
                expect(response.statusCode).toEqual(201);
                postedItems.push(response.body._links.self.href);
                return utils.httpPost(channelResource, headers, moment.utc().toISOString())
            })
            .then(response => {
                expect(response.statusCode).toEqual(201);
                postedItems.push(response.body._links.self.href);
                return utils.httpPost(channelResource, headers, moment.utc().toISOString())
            })
            .then(response => {
                expect(response.statusCode).toEqual(201);
                postedItems.push(response.body._links.self.href);
                return utils.httpPost(channelResource, headers, moment.utc().toISOString())
            })
            .then(response => {
                expect(response.statusCode).toEqual(201);
                postedItems.push(response.body._links.self.href);
            })
            .finally(() => {
                console.log('postedItems:', postedItems);
                expect(postedItems.constructor).toBe(Array);
                expect(postedItems.length).toEqual(4);
                done();
            });
    });

    it('gets latest ascending items', (done) => {
        let order = trimWordRandomly('ascending');
        utils.httpGet(`${channelResource}/latest/4?stable=false&order=${order}`)
            .then(expectURIsInAscendingOrder)
            .finally(done);
    });

    it('gets latest baloney order items', (done) => {
        let order = 'baloney';
        utils.httpGet(`${channelResource}/latest/4?stable=false&order=${order}`)
            .then(expectURIsInAscendingOrder)
            .finally(done);
    });

    it('gets descending earliest', (done) => {
        // secondDo(`${channelResource}/earliest/4?stable=false&order=desc`, descendingItems, done);
        let order = trimWordRandomly('descending');
        utils.httpGet(`${channelResource}/earliest/4?stable=false&order=${order}`)
            .then(expectURIsInDescendingOrder)
            .finally(done);
    });

    it('gets descending latest', (done) => {
        let order = trimWordRandomly('descending');
        utils.httpGet(`${channelResource}/latest/4?stable=false&order=${order}`)
            .then(expectURIsInDescendingOrder)
            .finally(done);
    });

    it('gets descending next', (done) => {
        let oneMinuteAgo = moment.utc().subtract(1, 'minute').format('YYYY/MM/DD/HH/mm/ss/SSS');
        let order = trimWordRandomly('descending');
        utils.httpGet(`${channelResource}/${oneMinuteAgo}/A/next/4?stable=false&order=${order}`)
            .then(expectURIsInDescendingOrder)
            .finally(done);
    });

    it('gets descending previous', (done) => {
        let oneMinuteInTheFuture = moment.utc().add(1, 'minute').format('YYYY/MM/DD/HH/mm/ss/SSS');
        let order = trimWordRandomly('descending');
        utils.httpGet(`${channelResource}/${oneMinuteInTheFuture}/A/previous/4?stable=false&order=${order}`)
            .then(expectURIsInDescendingOrder)
            .finally(done);
    });

    it('gets descending hour', (done) => {
        let now = moment.utc().format('YYYY/MM/DD/HH');
        let order = trimWordRandomly('descending');
        utils.httpGet(`${channelResource}/${now}?stable=false&order=${order}`)
            .then(expectURIsInDescendingOrder)
            .finally(done);
    });

    it('bulk get', (done) => {
        let oneMinuteAgo = moment.utc().subtract(1, 'minute').format('YYYY/MM/DD/HH/mm/ss/SSS');
        let order = trimWordRandomly('descending');
        let url = `${channelResource}/${oneMinuteAgo}/A/next/4?stable=false&order=${order}&bulk=true`;
        utils.httpGet(url)
            .then(response => {
                expect(response.statusCode).toBe(200);
                console.log(response.body);

                let descendingItems = postedItems.slice().reverse();
                let first = response.body.indexOf(descendingItems[0]);
                let second = response.body.indexOf(descendingItems[1]);
                let third = response.body.indexOf(descendingItems[2]);
                let fourth = response.body.indexOf(descendingItems[3]);

                // all the items should be present
                expect(first).not.toEqual(-1);
                expect(second).not.toEqual(-1);
                expect(third).not.toEqual(-1);
                expect(fourth).not.toEqual(-1);

                // they should be in the correct order
                expect(first).toBeLessThan(second);
                expect(second).toBeLessThan(third);
                expect(third).toBeLessThan(fourth);
            })
            .finally(done);
    });

    function expectURIsInAscendingOrder(response) {
        expect(response.statusCode).toBe(200);
        let uris = response.body._links.uris;
        console.log('uris:', uris);
        expect(uris).toEqual(postedItems);
    }

    function expectURIsInDescendingOrder(response) {
        expect(response.statusCode).toBe(200);
        let uris = response.body._links.uris;
        console.log('uris:', uris);
        let reversedItems = postedItems.slice().reverse();
        expect(uris).toEqual(reversedItems);
    }

});

function trimWordRandomly(word) {
    let trimLocation = Math.floor(Math.random() * (word.length - 2)) + 2;
    return word.slice(0, trimLocation);
}
