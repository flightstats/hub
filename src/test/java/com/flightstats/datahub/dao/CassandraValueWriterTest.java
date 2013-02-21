package com.flightstats.datahub.dao;

import com.flightstats.datahub.model.DataHubCompositeValue;
import com.flightstats.datahub.model.ValueInsertionResult;
import me.prettyprint.cassandra.serializers.StringSerializer;
import me.prettyprint.cassandra.serializers.UUIDSerializer;
import me.prettyprint.hector.api.beans.HColumn;
import me.prettyprint.hector.api.mutation.Mutator;
import org.junit.Test;

import java.util.Date;
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
        Date date = new Date(2345678910L);
        String contentType = "text/plain";
        DataHubCompositeValue value = new DataHubCompositeValue(contentType, data);
        ValueInsertionResult expected = new ValueInsertionResult(uuid, date);

        CassandraConnector connector = mock(CassandraConnector.class);
        HectorFactoryWrapper hector = mock(HectorFactoryWrapper.class);
        RowKeyStrategy<String, UUID, DataHubCompositeValue> rowStrategy = mock(RowKeyStrategy.class);
        Mutator mutator = mock(Mutator.class);
        HColumn<UUID, DataHubCompositeValue> column = mock(HColumn.class);

        when(connector.buildMutator(StringSerializer.get())).thenReturn(mutator);
        when(hector.getUniqueTimeUUIDinMillis()).thenReturn(uuid);
        when(hector.getDateFromUUID(uuid)).thenReturn(date);
        when(hector.createColumn(uuid, value, UUIDSerializer.get(), DataHubCompositeValueSerializer.get())).thenReturn(column);
        when(rowStrategy.buildKey(channelName, uuid)).thenReturn(key);

        CassandraValueWriter testClass = new CassandraValueWriter(connector, hector, rowStrategy);
        ValueInsertionResult result = testClass.write(channelName, value);

        assertEquals(expected, result);
        verify(mutator).insert(key, channelName, column);
    }
}
