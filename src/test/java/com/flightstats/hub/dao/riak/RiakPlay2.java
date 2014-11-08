package com.flightstats.hub.dao.riak;

import com.basho.riak.client.api.RiakClient;
import com.basho.riak.client.api.cap.Quorum;
import com.basho.riak.client.api.commands.indexes.IntIndexQuery;
import com.basho.riak.client.api.commands.kv.StoreValue;
import com.basho.riak.client.core.query.Location;
import com.basho.riak.client.core.query.Namespace;
import com.basho.riak.client.core.query.RiakObject;
import com.basho.riak.client.core.query.indexes.LongIntIndex;
import com.basho.riak.client.core.util.BinaryValue;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.util.List;

/**
 *
 */
public class RiakPlay2 {

    public static void main(String[] args) throws Exception {

        RiakClient client = RiakClient.newClient("127.0.0.1");

        DateTime now = new DateTime(DateTimeZone.UTC);
        System.out.println("start at  " + now);

        Namespace namespace = new Namespace("default", "my_bucket6");
        for (int i = 0; i < 24; i++) {
            now = now.withHourOfDay(i);
            String key = now.toString() + "-A";
            Location location = new Location(namespace, key);
            RiakObject riakObject = new RiakObject();
            riakObject.setValue(BinaryValue.create("time=" + now.getMillis()));
            riakObject.getIndexes().getIndex(LongIntIndex.named("time")).add(now.getMillis());
            StoreValue store = new StoreValue.Builder(riakObject)
                    .withLocation(location)
                    .withOption(StoreValue.Option.W, Quorum.quorumQuorum()).build();
            client.execute(store);
        }

        long start = now.withHourOfDay(8).getMillis();
        long end = now.withHourOfDay(15).getMillis();
        IntIndexQuery query = new IntIndexQuery.Builder(namespace, "time", start, end)
                .withPaginationSort(true)
                .withKeyAndIndex(true)
                .build();

        IntIndexQuery.Response queryResponse = client.execute(query);
        List<IntIndexQuery.Response.Entry> entries = queryResponse.getEntries();
        for (IntIndexQuery.Response.Entry entry : entries) {

            System.out.println("query response " + entry.getRiakObjectLocation().getKey() + " " + entry.getIndexKey());
        }
    }
}
