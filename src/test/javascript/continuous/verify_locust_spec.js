require('../integration_config.js');

const MonkeyPatchingConsole = require('console.table');
const locustURL = `http://${process.env.locustUrl}`;

/**
 * - get stats from a locust instance
 * - log stats to console
 * - verify no failures
 * - reset the stats
 */

describe(__filename, () => {

  let stats;

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

        let statsSansTotal = response.body.stats.filter(s => s.name !== 'Total');
        console.table('stats', sort(statsSansTotal, sortByStatKeys));
        console.table('errors', sort(response.body.errors, sortByErrorKeys));

        stats = statsSansTotal;
      })
      .finally(done);
  });

  it('verifies there are no failures', () => {
    expect(stats).toBeDefined();
    let failures = stats.reduce((output, stat) => output + stat.num_failures, 0);
    console.log('failures:', failures);
    expect(failures).toBe(0);
  });

  it('resets stats', (done) => {
    utils.httpGet(`${locustURL}/stats/reset`)
      .then(response => expect(response.statusCode).toBe(200))
      .finally(done);
  });

});

function sort(arrayOfObjects, comparator) {
  return arrayOfObjects.map(object => {
    let sortedObject = {};
    Object.keys(object)
      .sort(comparator)
      .forEach(key => sortedObject[key] = object[key]);
    return sortedObject;
  });
}

function sortByStatKeys(a, b) {
  if (a === 'method') return -1;
  if (b === 'method') return 1;
  if (a === 'name') return -1;
  if (b === 'name') return 1;
  if (a === 'num_requests') return -1;
  if (b === 'num_requests') return 1;
  if (a === 'num_failures') return -1;
  if (b === 'num_failures') return 1;
  return 0;
}

function sortByErrorKeys(a, b) {
  if (a === 'method') return -1;
  if (b === 'method') return 1;
  if (a === 'name') return -1;
  if (b === 'name') return 1;
  if (a === 'occurences') return -1;
  if (b === 'occurences') return 1;
  return 0;
}
