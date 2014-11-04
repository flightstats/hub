package com.flightstats.hub.dao.riak;

import com.basho.riak.client.api.RiakClient;
import com.basho.riak.client.api.cap.Quorum;
import com.basho.riak.client.api.commands.indexes.BinIndexQuery;
import com.basho.riak.client.api.commands.kv.FetchValue;
import com.basho.riak.client.api.commands.kv.StoreValue;
import com.basho.riak.client.core.query.Location;
import com.basho.riak.client.core.query.Namespace;
import com.basho.riak.client.core.query.RiakObject;
import com.basho.riak.client.core.query.indexes.StringBinIndex;
import com.basho.riak.client.core.util.BinaryValue;

import java.util.List;

/**
 *
 */
public class RiakPlay {

    public static void main(String[] args) throws Exception {
        RiakClient client = RiakClient.newClient("127.0.0.1");

        Namespace namespace = new Namespace("default", "my_bucket");
        String key = "my_key_indexed";
        Location location = new Location(namespace, key);
        RiakObject riakObject = new RiakObject();
        riakObject.setValue(BinaryValue.create("my_value"));
        riakObject.getIndexes().getIndex(StringBinIndex.named("twitter")).add("jsmith123");
        riakObject.getIndexes().getIndex(StringBinIndex.named("email")).add("jsmith@basho.com");

        StoreValue store = new StoreValue.Builder(riakObject)
                .withLocation(location)
                .withOption(StoreValue.Option.W, new Quorum(3)).build();
        client.execute(store);
        System.out.println("stored object!");

        FetchValue.Response response = client.execute(new FetchValue.Builder(location).build());
        RiakObject object = response.getValue(RiakObject.class);
        System.out.println("found object " + object.getValue().toStringUtf8());


        BinIndexQuery binIndexQuery = new BinIndexQuery.Builder(namespace, "twitter", "jsmith123").build();
        BinIndexQuery.Response queryResponse = client.execute(binIndexQuery);
        List<BinIndexQuery.Response.Entry> entries = queryResponse.getEntries();
        for (BinIndexQuery.Response.Entry entry : entries) {
            System.out.println("query response " + entry.getRiakObjectLocation().getKey());
        }
    }
}
