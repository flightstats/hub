require('../integration_config.js');

const async = require('async');
const moment = require('moment');
const MonkeyPatchingConsole = require('console.table');

/**
 * - get stats from a locust instance
 * - log stats to console
 * - verify no failures
 * - reset the stats
 */

const locustURL = `http://${process.env.locustUrl}`;

describe(__filename, () => {

    let totalFailures;

    it('get the current stats', (done) => {
        utils.httpGet(`${locustURL}/stats/requests`, {'Accept': 'application/json'})
            .then(response => {
                expect(response.statusCode).toBe(200);
                console.log();
                console.log('locust:', locustURL);
                console.log('state:', response.body.state);
                console.log('users:', response.body.user_count);
                console.log('failures:', Math.round(response.body.fail_ratio * 100) + '%');
                console.log('requests/sec:', response.body.total_rps);
                console.log();
                console.table(response.body.stats);
                console.table(response.body.errors);
                totalFailures = response.body.stats.reduce((output, stat) => output + stat.num_failures, 0);
            })
            .finally(done);
    });

    it('verifies there are no failures', () => {
        console.log('total failures:', totalFailures);
        expect(totalFailures).toBeDefined();
        expect(totalFailures).toBe(0);
    });

    it('resets stats', (done) => {
        utils.httpGet(`${locustURL}/stats/reset`)
            .then(response => expect(response.statusCode).toBe(200))
            .finally(done);
    });

});
