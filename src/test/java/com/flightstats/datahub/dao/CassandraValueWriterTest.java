package com.flightstats.datahub.dao;

import me.prettyprint.cassandra.serializers.BytesArraySerializer;
import me.prettyprint.cassandra.serializers.StringSerializer;
import me.prettyprint.cassandra.serializers.UUIDSerializer;
import me.prettyprint.hector.api.beans.HColumn;
import me.prettyprint.hector.api.mutation.Mutator;
import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

public class CassandraValueWriterTest {

    @Test
    public void testInsert() throws Exception {
        String channelName = "foo";
        byte[] data = "bar".getBytes();
        UUID uuid = UUID.randomUUID();
        String key = "a super key for this row";

        CassandraConnector connector = mock(CassandraConnector.class);
        HectorFactoryWrapper hector = mock(HectorFactoryWrapper.class);
        RowKeyStrategy<String, UUID, byte[]> rowStrategy = mock(RowKeyStrategy.class);
        Mutator mutator = mock(Mutator.class);
        HColumn<UUID, byte[]> column = mock(HColumn.class);

        when(connector.buildMutator(StringSerializer.get())).thenReturn(mutator);
        when(hector.getUniqueTimeUUIDinMillis()).thenReturn(uuid);
        when(hector.createColumn(uuid, data, UUIDSerializer.get(), BytesArraySerializer.get())).thenReturn(column);
        when(rowStrategy.buildKey(channelName, uuid)).thenReturn(key);

        CassandraValueWriter testClass = new CassandraValueWriter(connector, hector, rowStrategy);
        UUID result = testClass.write(channelName, data);

        assertEquals(uuid, result);
        verify(mutator).insert(key, channelName, column);
    }
}
