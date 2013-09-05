/**
 * Created with JetBrains WebStorm.
 * User: gnewcomb
 * Date: 3/4/13
 * Time: 9:51 AM
 * To change this template use File | Settings | File Templates.
 */

var chai = require('chai');
var expect = chai.expect;

var dhh = require('../DH_test_helpers/DHtesthelpers.js');


describe('Health check:', function() {

    it('/health returns 200', function(done) {
        dhh.getHealth({}, function(res) {
            expect(res.status).to.equal(200);
            done();
        });
    });
});
