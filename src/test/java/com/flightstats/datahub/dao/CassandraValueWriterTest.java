package com.flightstats.datahub.dao;

import com.flightstats.datahub.model.DataHubCompositeValue;
import com.flightstats.datahub.model.DataHubKey;
import com.flightstats.datahub.model.ValueInsertionResult;
import com.flightstats.datahub.util.DataHubKeyGenerator;
import me.prettyprint.cassandra.serializers.StringSerializer;
import me.prettyprint.hector.api.beans.HColumn;
import me.prettyprint.hector.api.mutation.Mutator;
import org.junit.Test;

import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

public class CassandraValueWriterTest {

    @Test
    public void testInsert() throws Exception {
        String channelName = "foo";
        byte[] data = "bar".getBytes();
        Date date = new Date(2345678910L);
        DataHubKey key = new DataHubKey(date, (short) 33);
        String rowKey = "a super key for this row";
        String contentType = "text/plain";
        DataHubCompositeValue value = new DataHubCompositeValue(contentType, data);
        ValueInsertionResult expected = new ValueInsertionResult(key);
        String columnName = key.asSortableString();

        CassandraConnector connector = mock(CassandraConnector.class);
        HectorFactoryWrapper hector = mock(HectorFactoryWrapper.class);
        RowKeyStrategy<String, String, DataHubCompositeValue> rowStrategy = mock(RowKeyStrategy.class);
        Mutator mutator = mock(Mutator.class);
        HColumn<String, DataHubCompositeValue> column = mock(HColumn.class);
        DataHubKeyGenerator keyGenerator = mock(DataHubKeyGenerator.class);

        when(connector.buildMutator(StringSerializer.get())).thenReturn(mutator);
        when(hector.createColumn(columnName, value, StringSerializer.get(), DataHubCompositeValueSerializer.get())).thenReturn(column);
        when(rowStrategy.buildKey(channelName, columnName)).thenReturn(rowKey);
        when(keyGenerator.newKey()).thenReturn(key);

        CassandraValueWriter testClass = new CassandraValueWriter(connector, hector, rowStrategy, keyGenerator);
        ValueInsertionResult result = testClass.write(channelName, value);

        assertEquals(expected, result);
        verify(mutator).insert(rowKey, channelName, column);
    }
}
