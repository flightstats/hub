package com.flightstats.datahub.dao;

import com.flightstats.datahub.dao.serialize.DataHubCompositeValueSerializer;
import com.flightstats.datahub.model.DataHubCompositeValue;
import com.flightstats.datahub.model.DataHubKey;
import com.flightstats.datahub.model.ValueInsertionResult;
import com.flightstats.datahub.model.exception.NoSuchChannelException;
import com.flightstats.datahub.util.DataHubKeyGenerator;
import com.flightstats.datahub.util.DataHubKeyRenderer;
import com.google.common.base.Optional;
import me.prettyprint.cassandra.serializers.StringSerializer;
import me.prettyprint.hector.api.beans.HColumn;
import me.prettyprint.hector.api.exceptions.HInvalidRequestException;
import me.prettyprint.hector.api.mutation.Mutator;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static com.flightstats.datahub.dao.CassandraChannelsCollection.DATA_HUB_COLUMN_FAMILY_NAME;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

public class CassandraValueWriterTest {

	public static final String CHANNEL_NAME = "foo";
	public static final byte[] DATA = "bar".getBytes();
	public static final DataHubKey DATA_HUB_KEY = new DataHubKey(new Date(2345678910L), (short) 33);
	public static final String ROW_KEY = "a super key for this row";
	public static final Optional<String> CONTENT_TYPE = Optional.of("text/plain");
	public static final Optional<String> CONTENT_ENCODING = Optional.of("gzip");
	public static final Optional<String> CONTENT_LANGUAGE = Optional.absent();

	private CassandraConnector connector;
	private HectorFactoryWrapper hector;
	private Mutator<String> mutator;
	private RowKeyStrategy<String, DataHubKey, DataHubCompositeValue> rowStrategy;
	private HColumn<String, DataHubCompositeValue> column;
	private DataHubKeyGenerator keyGenerator;
	private DataHubKeyRenderer keyRenderer;

	@Before
	public void setup() {
		connector = mock(CassandraConnector.class);
		hector = mock(HectorFactoryWrapper.class);
		rowStrategy = mock(RowKeyStrategy.class);
		mutator = mock(Mutator.class);
		column = mock(HColumn.class);
		keyGenerator = mock(DataHubKeyGenerator.class);
		keyRenderer = new DataHubKeyRenderer();

		when(connector.buildMutator(StringSerializer.get())).thenReturn(mutator);
	}

	@Test
	public void testInsert() throws Exception {
		DataHubCompositeValue value = new DataHubCompositeValue(CONTENT_TYPE, CONTENT_LANGUAGE, DATA);
		ValueInsertionResult expected = new ValueInsertionResult(DATA_HUB_KEY);
		String columnName = keyRenderer.keyToString(DATA_HUB_KEY);

		when(hector.createColumn(columnName, value, StringSerializer.get(), DataHubCompositeValueSerializer.get())).thenReturn(column);
		when(rowStrategy.buildKey(CHANNEL_NAME, DATA_HUB_KEY)).thenReturn(ROW_KEY);
		when(keyGenerator.newKey(CHANNEL_NAME)).thenReturn(DATA_HUB_KEY);

		CassandraValueWriter testClass = new CassandraValueWriter(connector, hector, rowStrategy, keyGenerator, keyRenderer);
		ValueInsertionResult result = testClass.write(CHANNEL_NAME, value);

		assertEquals(expected, result);
		verify(mutator).insert(ROW_KEY, DATA_HUB_COLUMN_FAMILY_NAME, column);
	}

	@Test(expected = NoSuchChannelException.class)
	public void testInsertWithMissingChannel() throws Exception {
		DataHubCompositeValue value = new DataHubCompositeValue(CONTENT_TYPE, CONTENT_LANGUAGE, DATA);
		String columnName = keyRenderer.keyToString(DATA_HUB_KEY);

		when(hector.createColumn(columnName, value, StringSerializer.get(), DataHubCompositeValueSerializer.get())).thenReturn(column);
		when(rowStrategy.buildKey(CHANNEL_NAME, DATA_HUB_KEY)).thenReturn(ROW_KEY);
		when(keyGenerator.newKey(CHANNEL_NAME)).thenReturn(DATA_HUB_KEY);
		when(mutator.insert(ROW_KEY, DATA_HUB_COLUMN_FAMILY_NAME, column)).thenThrow(
                new HInvalidRequestException("You must have an unconfigured columnfamily in your soup"));

		CassandraValueWriter testClass = new CassandraValueWriter(connector, hector, rowStrategy, keyGenerator, keyRenderer);
		testClass.write(CHANNEL_NAME, value);
	}

	@Test(expected = HInvalidRequestException.class)
	public void testOtherExceptionMessages() throws Exception {
		DataHubCompositeValue value = new DataHubCompositeValue(CONTENT_TYPE, CONTENT_LANGUAGE, DATA);
		String columnName = keyRenderer.keyToString(DATA_HUB_KEY);

		when(hector.createColumn(columnName, value, StringSerializer.get(), DataHubCompositeValueSerializer.get())).thenReturn(column);
		when(rowStrategy.buildKey(CHANNEL_NAME, DATA_HUB_KEY)).thenReturn(ROW_KEY);
		when(keyGenerator.newKey(CHANNEL_NAME)).thenReturn(DATA_HUB_KEY);
		when(mutator.insert(ROW_KEY, DATA_HUB_COLUMN_FAMILY_NAME, column)).thenThrow(
                new HInvalidRequestException("Clown-based-tamale"));            //Not the expected verbage

		CassandraValueWriter testClass = new CassandraValueWriter(connector, hector, rowStrategy, keyGenerator, keyRenderer);
		testClass.write(CHANNEL_NAME, value);
	}

	@Test
	public void testDelete() throws Exception {
		List<DataHubKey> keys = Arrays.asList( DATA_HUB_KEY, DATA_HUB_KEY );
		String columnName = keyRenderer.keyToString(DATA_HUB_KEY);

		when(rowStrategy.buildKey(CHANNEL_NAME, DATA_HUB_KEY)).thenReturn(ROW_KEY);
		when(keyGenerator.newKey(CHANNEL_NAME)).thenReturn(DATA_HUB_KEY);

		CassandraValueWriter testClass = new CassandraValueWriter(connector, hector, rowStrategy, keyGenerator, keyRenderer);
		testClass.delete(CHANNEL_NAME, keys);

		verify(mutator, times(2)).addDeletion(ROW_KEY, CHANNEL_NAME, columnName, StringSerializer.get());
		verify(mutator).execute();
	}
}
