require('./integration_config.js');

var request = require('request');
var _ = require('lodash');
var channelName = utils.randomChannelName();
var channelResource = channelUrl + "/" + channelName;
var testName = __filename;
utils.configureFrisby();


/**
 * create a global channel via put
 * verify that it exists on Master
 * verify that it exists on Satellite
 */
describe(testName, function () {

    var channel = {
        name: channelName,
        ttlDays: 1,
        global: {
            master: hubUrlBase,
            satellites: [satelliteUrl]
        }
    };

    var verify = function (response, body, hubUrl) {
        hubUrl = hubUrl || hubUrlBase;
        var parse = utils.parseJson(response, testName);
        console.log('parsed ', parse)
        console.log('hubUrl ', hubUrl)
        expect(parse._links.self.href).toEqual(hubUrl + '/channel/' + channelName);
        expect(parse.ttlDays).toEqual(1);
        expect(parse.description).toEqual('');
        expect(parse.tags.length).toEqual(1);
        expect(parse.tags).toContain('global');
        expect(parse.replicationSource).toEqual('');
        expect(parse.global.master).toEqual(channel.global.master + '/')
        expect(parse.global.satellites[0]).toEqual(channel.global.satellites[0] + '/')
    };

    utils.putChannel(channelName, verify, channel, testName);

    utils.getChannel(channelName, verify, testName);

    utils.sleep(50);

    utils.getChannel(channelName, verify, testName, satelliteUrl);

    var globalGroup = 'Global_' + satelliteDomain.replace (/\./g, "_") + '_' + channelName;
    console.log('look at global group', globalGroup);
    utils.getGroup(globalGroup, {}, 200, function () {});

});
