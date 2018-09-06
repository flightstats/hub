const {
    getProp,
    hubClientGet,
} = require('../lib/helpers');
require('console.table');
const locustURL = `http://${process.env.locustUrl}`;
const headers = { 'Content-Type': 'application/json' };
let stats = [];
/**
 * - get stats from a locust instance
 * - log stats to console
 * - verify no failures
 * - reset the stats
 */

const sort = (arrayOfObjects, comparator) => arrayOfObjects.map(object => Object.keys(object)
    .sort(comparator)
    .reduce((accum, key) => {
        accum[key] = object[key];
        return accum;
    }, {}));

const sortByStatKeys = (a, b) => {
    const values = ['method', 'name', 'num_requests', 'num_failures'];
    if (values.includes(a)) return -1;
    if (values.includes(b)) return 1;
    return 0;
};

const sortByErrorKeys = (a, b) => {
    const values = ['method', 'name', 'occurences'];
    if (values.includes(a)) return -1;
    if (values.includes(b)) return 1;
    return 0;
};

describe(__filename, () => {
    it('get the current stats', async () => {
        const response = await hubClientGet(`${locustURL}/stats/requests`, headers);
        expect(getProp('statusCode', response)).toBe(200);
        console.log('locust:', locustURL);
        const body = getProp('body', response) || {};
        console.log('state:', body.state);
        console.log('users:', body.user_count);
        console.log('failures:', Math.round(body.fail_ratio * 100), '%');
        console.log('requests/sec:', body.total_rps);
        console.log();
        const bodyStats = getProp('stats', body) || [];
        const errors = getProp('errors', body) || [];
        const statsSansTotal = bodyStats.filter(s => s.name !== 'Total');
        console.table('stats', sort(statsSansTotal, sortByStatKeys));
        console.table('errors', sort(errors, sortByErrorKeys));
        stats = statsSansTotal;
    });

    it('verifies there are no failures', () => {
        expect(stats).toBeDefined();
        const failures = stats.reduce((output, stat) => (output + getProp('num_failures', stat)), 0);
        console.log('failures:', failures);
        expect(failures).toBe(0);
    });

    it('resets stats', async () => {
        const response = await hubClientGet(`${locustURL}/stats/reset`, headers);
        expect(getProp('statusCode', response)).toBe(200);
    });
});
