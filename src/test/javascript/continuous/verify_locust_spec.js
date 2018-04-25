require('../integration_config.js');
require('console.table');
let locustURL = `http://${process.env.locustUrl}`;
console.log(locustURL);

/**
 * - Pull down the current stats from a running Locust host
 * - Log the stats
 * - Verify there are no failures
 * - Reset the stats
 */

describe(__filename, () => {

  it('loads results', (done) => {
    utils.httpGet(`${locustURL}/stats/requests`, {'Accept': 'application/json'})
      .then(response => {
        console.table(response.body.stats);
        console.table(response.body.errors);

        let failedItems = response.body.stats.filter(item => item.num_failures > 0);
        failedItems.forEach(item => {
          console.log('failures:', item.type, item.name, item._num_failures);
        });

        expect(failedItems.length).toEqual(0);
      })
      .finally(done);
  });

  it('resets stats', (done) => {
    utils.httpGet(`${locustURL}/stats/reset`)
      .then(response => expect(response.statusCode).toBe(200))
      .finally(done);
  });

});
