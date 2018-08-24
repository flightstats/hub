const request = require('request');
const moment = require('moment');
const {
    createChannel,
    fromObjectPath,
    getProp,
    randomChannelName,
} = require('../lib/helpers');
const { getChannelUrl } = require('../lib/config');

const channelUrl = getChannelUrl();
const channelName = randomChannelName();
const channelResource = `${channelUrl}/${channelName}`;
let createdChannel = false;
/**
 * This should:
 *
 * 1 - create a channel
 * 2 - get time links for that channel
 * 3 - verify that verify templates are correct
 */
describe(__filename, function () {
    beforeAll(async () => {
        const channel = await createChannel(channelName);
        if (getProp('statusCode', channel) === 201) {
            createdChannel = true;
            console.log(`created channel for ${__filename}`);
        }
    });

    function verifyLinks (body, url, time, params) {
        params = params || '';
        if (body && body._links) {
            const selfLink = fromObjectPath(['_links', 'self', 'href'], body);
            const minute = fromObjectPath(['_links', 'minute'], body) || {};
            const second = fromObjectPath(['_links', 'second'], body) || {};
            const hour = fromObjectPath(['_links', 'hour'], body) || {};
            const day = fromObjectPath(['_links', 'day'], body) || {};
            expect(selfLink).toBe(url + params);
            expect(second.template).toBe(url + '/{year}/{month}/{day}/{hour}/{minute}/{second}{?stable}');
            expect(second.redirect).toBe(url + '/second' + params);

            expect(minute.href).toBe(channelResource + time.format('/YYYY/MM/DD/HH/mm') + params);
            expect(minute.template).toBe(url + '/{year}/{month}/{day}/{hour}/{minute}{?stable}');
            expect(minute.redirect).toBe(url + '/minute' + params);

            expect(hour.href).toBe(channelResource + time.format('/YYYY/MM/DD/HH') + params);
            expect(hour.template).toBe(url + '/{year}/{month}/{day}/{hour}{?stable}');
            expect(hour.redirect).toBe(url + '/hour' + params);

            expect(day.href).toBe(channelResource + time.format('/YYYY/MM/DD') + params);
            expect(day.template).toBe(url + '/{year}/{month}/{day}{?stable}');
            expect(day.redirect).toBe(url + '/day' + params);
        } else {
            expect(!!body && !!body._links).toBe(true);
        }
    }

    it('gets stable time links', function (done) {
        if (!createdChannel) return done.fail('channel not created in before block');
        const url = channelResource + '/time';
        request.get({url: url, json: true},
            function (err, response, body) {
                expect(err).toBeNull();
                expect(getProp('statusCode', response)).toBe(200);
                const millis = fromObjectPath(['stable', 'millis'], body);
                verifyLinks(body, url, moment(millis).utc());
                done();
            });
    });

    it('gets stable param time links', function (done) {
        if (!createdChannel) return done.fail('channel not created in before block');
        const url = channelResource + '/time';
        const params = '?stable=true';
        request.get({url: url + params, json: true},
            function (err, response, body) {
                expect(err).toBeNull();
                expect(getProp('statusCode', response)).toBe(200);
                const millis = fromObjectPath(['stable', 'millis'], body);
                verifyLinks(body, url, moment(millis).utc(), params);
                done();
            });
    });

    it('gets unstable time links', function (done) {
        if (!createdChannel) return done.fail('channel not created in before block');
        const url = channelResource + '/time';
        const params = '?stable=false';
        request.get({url: url + params, json: true},
            function (err, response, body) {
                expect(err).toBeNull();
                expect(getProp('statusCode', response)).toBe(200);
                const millis = fromObjectPath(['now', 'millis'], body);
                verifyLinks(body, url, moment(millis).utc(), params);
                done();
            });
    });
});
