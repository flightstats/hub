package com.flightstats.datahub.dao;

import com.datastax.driver.core.*;

/**
 * If we upgrade to CQL3 in Cassandra2, we won't need this class to set the consistency all the time.
 * Since Session is not an interface, only added the methods we are using.
 *
 */
public class QuorumSession {

    private Session session;

    public QuorumSession(Session session) {
        this.session = session;
    }

    public ResultSet execute(String query) {
        PreparedStatement statement = session.prepare(query);
        return execute(statement.bind());
    }

    public ResultSet execute(Query query) {
        query.setConsistencyLevel(ConsistencyLevel.QUORUM);
        return session.execute(query);
    }

    public PreparedStatement prepare(String query) {
        return session.prepare(query);
    }
}
