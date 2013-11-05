package com.flightstats.datahub.service;

import com.flightstats.datahub.app.DataHubMain;
import org.cassandraunit.utils.EmbeddedCassandraServerHelper;

/**
 * The goal of this is to test the interface from end to end, using all of the real technologies we can,
 * such as HazelCast, Cassandra, Jersey, etc.
 */
public class DataHubStarter {

    //todo - gfm - 11/5/13 - this can probably go away
    public static void main(String[] args) throws Exception {
        EmbeddedCassandraServerHelper.startEmbeddedCassandra();
        args = new String[]{"/Users/gmoulliet/code/datahub/datahub-service/src/test/conf/datahub.properties"};
        DataHubMain.main(args);
    }


}
