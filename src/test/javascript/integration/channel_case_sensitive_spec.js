require('../integration_config');
const moment = require('moment');
const {
    fromObjectPath,
    getProp,
    hubClientGet,
    hubClientPostTestItem,
    hubClientPut,
} = require('../lib/helpers');

const headers = { 'Content-type': 'application/json' };
const channelName = utils.randomChannelName() + '_AbCdE';
const channelResource = `${channelUrl}/${channelName}`;
let posted = null;
const lowerCase = `${channelUrl}/${channelName.toLowerCase()}`;
const upperCase = `${channelUrl}/${channelName.toUpperCase()}`;
const startTime = moment.utc().subtract(1, 'minute');

describe(__filename, function () {
    it(`creates channel ${channelName} at ${channelUrl}`, async () => {
        console.log('startTime', startTime.format('/YYYY/MM/DD/HH/mm/ss/SSS'));
        const response = await hubClientPut(channelResource, headers, { ttlDays: 1 });
        expect(getProp('statusCode', response)).toBe(201);
    });

    const verifyTwoUrlsInData = async (channelUrl, path, done) => {
        const response1 = await hubClientGet(`${channelUrl}${path}`, headers);
        expect(getProp('statusCode', response1)).toBe(200);
        const body = getProp('body', response1);
        const uris = fromObjectPath(['_links', 'uris'], body) || [];
        expect(uris.length).toBe(2);
        expect(uris[1]).toContain(channelUrl);
        expect(uris[0]).toContain(channelUrl);
        const response2 = await hubClientGet(uris[0], headers);
        expect(getProp('statusCode', response2)).toBe(200);
        expect(getProp('body', response2)).toBeDefined();
    };

    it(`verifies channel lower case ${lowerCase}`, async () => {
        const response = await hubClientGet(lowerCase);
        expect(getProp('statusCode', response)).toEqual(200);
        expect(getProp('body', response)).toContain(lowerCase);
    });

    it(`verifies channel lower case ${lowerCase}`, async () => {
        const response = await hubClientGet(`${lowerCase}?cached=false`);
        expect(getProp('statusCode', response)).toEqual(200);
        expect(getProp('body', response)).toContain(lowerCase);
    });

    it(`verifies channel upper case ${upperCase}`, async () => {
        const response = await hubClientGet(upperCase);
        expect(getProp('statusCode', response)).toEqual(200);
        expect(getProp('body', response)).toContain(upperCase);
    });

    it(`verifies channel upper case ${upperCase}`, async () => {
        const response = await hubClientGet(`${upperCase}?cached=false`);
        expect(getProp('statusCode', response)).toEqual(200);
        expect(getProp('body', response)).toContain(upperCase);
    });

    it(`posts an item to channel ${upperCase}`, async () => {
        const response = await hubClientPostTestItem(`${upperCase}?forceWrite=true`);
        posted = fromObjectPath(['headers', 'location'], response);
        expect(getProp('statusCode', response)).toEqual(201);
    });

    it(`verifies get upper case ${upperCase} data`, async () => {
        const response = await hubClientGet(`${posted}?cached=false`);
        expect(getProp('statusCode', response)).toEqual(200);
        expect(getProp('body', response)).toContain('"data');
    });

    it(`posts an item to channel ${lowerCase}`, async () => {
        const response = await hubClientPostTestItem(`${lowerCase}?forceWrite=true`);
        expect(getProp('statusCode', response)).toEqual(201);
    });

    /*
      it(`gets time hour ${lowerCase}`, function (done) {
        verifyTwoUrlsInData(lowerCase, '/time/hour?stable=false', done);
      });

    it("gets time hour " + upperCase, function (done) {
        verifyTwoUrlsInData(upperCase, '/time/hour?stable=false', done);
     });
     */

    // this delay is to allow the item time for the S3 write.
    utils.itSleeps(5000);

    it(`gets latest 2 ${upperCase}`, async () => {
        await verifyTwoUrlsInData(upperCase, '/latest/2?stable=false');
    });

    /*
      it(`gets time hour LONG_TERM_SINGLE ${lowerCase}`, function (done) {
        verifyTwoUrlsInData(lowerCase, '/time/hour?location=LONG_TERM_SINGLE&stable=false&trace=true', done);
    });

    it("gets first url remote ", function (done) {
        getUrl(uris[0] + '?remoteOnly=true', done);
    });

    it("gets second url remote ", function (done) {
        getUrl(uris[1] + '?remoteOnly=true', done);
     });
     */

    it(`gets next 2 ${lowerCase}`, async () => {
        await verifyTwoUrlsInData(lowerCase, `${startTime.format('/YYYY/MM/DD/HH/mm/ss/SSS')}/A/next/2?stable=false&trace=true`);
    });

    it(`gets next 2 ${upperCase}`, async () => {
        await verifyTwoUrlsInData(upperCase, `${startTime.format('/YYYY/MM/DD/HH/mm/ss/SSS')}/A/next/2?stable=false&trace=true`);
    });

    /*
    it(`gets prev 2 ${lowerCase}`, function (done) {
        verifyTwoUrlsInData(lowerCase, moment.utc().format('/YYYY/MM/DD/HH/mm/ss/SSS') + '/A/prev/2?stable=false', done);
    });

    it(`gets prev 2 ${upperCase}`, function (done) {
        verifyTwoUrlsInData(upperCase, moment.utc().format('/YYYY/MM/DD/HH/mm/ss/SSS') + '/A/prev/2?stable=false', done);
    });

    it(`gets earliest 2 ${lowerCase}`, function (done) {
        verifyTwoUrlsInData(lowerCase, '/earliest/2?stable=false', done);
    });

    it(`gets earliest 2 ${upperCase}`, function (done) {
        verifyTwoUrlsInData(upperCase, '/earliest/2?stable=false', done);
     });
     */
});
